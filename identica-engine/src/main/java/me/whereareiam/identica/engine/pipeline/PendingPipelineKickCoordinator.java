package me.whereareiam.identica.engine.pipeline;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaShutdownEvent;
import me.whereareiam.identica.event.pipeline.state.PipelineStateClearedEvent;
import me.whereareiam.identica.event.pipeline.state.PipelineStateSavedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.delivery.DeliveryPayload;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.model.delivery.DeliveryTarget;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.scheduler.DelayedRunnableTask;
import me.whereareiam.identica.model.scheduler.JobKey;
import me.whereareiam.identica.model.scheduler.Origin;
import me.whereareiam.identica.model.scheduler.Purpose;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.service.Scheduler;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import me.whereareiam.identica.type.messaging.DeliverySemantics;
import me.whereareiam.identica.type.messaging.DeliverySource;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Singleton
public class PendingPipelineKickCoordinator implements EventListener {
	private static final Origin ORIGIN = Origin.core(PendingPipelineKickCoordinator.class);
	private static final Purpose PURPOSE = Purpose.of("pipeline-expiry");

	private final Provider<Messages> messagesProvider;
	private final IdentityService identityService;
	private final DeliveryService deliveryService;
	private final Scheduler scheduler;

	@Inject
	public PendingPipelineKickCoordinator(
			Provider<Messages> messagesProvider,
			IdentityService identityService,
			DeliveryService deliveryService,
			Scheduler scheduler,
			EventManager eventManager
	) {
		this.messagesProvider = messagesProvider;
		this.identityService = identityService;
		this.deliveryService = deliveryService;
		this.scheduler = scheduler;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onShutdown(@NotNull IdenticaShutdownEvent event) {
		scheduler.cancelByOrigin(ORIGIN);
	}

	@IdenticEvent
	public void onPipelineStateSaved(@NotNull PipelineStateSavedEvent event) {
		PipelineState state = event.getState();
		if (!isPending(state)) {
			cancel(event.getReference(), state.getPipelineType());
			return;
		}

		PipelineType type = state.getPipelineType();
		if (type == null) return;

		PipelineStateReference reference = event.getReference();
		long delayMs = Math.max(0L, event.getExpiresAt() - System.currentTimeMillis());
		JobKey key = jobKey(type, reference);
		scheduleKick(key, delayMs, reference, type);
	}

	@IdenticEvent
	public void onPipelineStateCleared(@NotNull PipelineStateClearedEvent event) {
		PipelineState state = event.getState();
		PipelineType type = state != null ? state.getPipelineType() : null;
		cancel(event.getReference(), type);
	}

	private void cancel(@NotNull PipelineStateReference reference, @Nullable PipelineType type) {
		if (type != null) {
			cancelByKey(jobKey(type, reference));
			return;
		}

		for (PipelineType candidate : PipelineType.values())
			cancelByKey(jobKey(candidate, reference));
	}

	private void scheduleKick(
			@NotNull JobKey key,
			long delayMs,
			@NotNull PipelineStateReference reference,
			@NotNull PipelineType type
	) {
		DelayedRunnableTask task = DelayedRunnableTask.builder()
				.key(key)
				.delay(delayMs)
				.runnable(() -> disconnectExpired(reference, type))
				.build();

		scheduler.schedule(task);
	}

	private void cancelByKey(@NotNull JobKey key) {
		scheduler.cancel(key);
	}

	private void disconnectExpired(@NotNull PipelineStateReference reference, @NotNull PipelineType type) {
		Identity identity = resolveIdentity(reference);
		if (identity == null) return;
		queueCancelledNotice(reference, type, identity);

		String message = resolveExpiredMessage(type);
		identity.disconnect(Serializer.serialize(identity, message));
	}

	private void queueCancelledNotice(
			@NotNull PipelineStateReference reference,
			@NotNull PipelineType type,
			@NotNull Identity identity
	) {
		if (type != PipelineType.MIGRATION) return;

		UUID accountUniqueId = reference.getAccountUniqueId();
		if (accountUniqueId == null) accountUniqueId = identity.getAccountUniqueId();
		if (accountUniqueId == null) return;

		deliveryService.queue(DeliveryRequest.builder()
				.id(UUID.randomUUID())
				.source(DeliverySource.NOTICE)
				.target(DeliveryTarget.builder()
						.accountUniqueId(accountUniqueId)
						.build())
				.payload(DeliveryPayload.builder()
						.chatMessage(String.join("\n", messagesProvider.get().getConnection().getMigration().getCancelled()))
						.build())
				.checkpoint(DeliveryCheckpoint.PLATFORM_READY_INITIAL)
				.semantics(DeliverySemantics.ONCE)
				.createdAt(System.currentTimeMillis())
				.updatedAt(System.currentTimeMillis())
				.build());
	}

	private @Nullable Identity resolveIdentity(@NotNull PipelineStateReference reference) {
		UUID connectionUniqueId = reference.getConnectionUniqueId();
		if (connectionUniqueId != null)
			return identityService.findByConnectionUniqueId(connectionUniqueId).orElse(null);

		UUID accountUniqueId = reference.getAccountUniqueId();
		return accountUniqueId != null ? identityService.findByAccountUniqueId(accountUniqueId).orElse(null) : null;
	}

	private @NotNull String resolveExpiredMessage(@NotNull PipelineType pipelineType) {
		Messages.Connection.Scenario scenario = resolveScenarioMessages(pipelineType);
		List<String> lines = scenario.getPipelineExpired();
		if (lines.isEmpty()) return "";
		return String.join("\n", lines);
	}

	private @NotNull Messages.Connection.Scenario resolveScenarioMessages(@NotNull PipelineType type) {
		Messages.Connection connection = messagesProvider.get().getConnection();

		if (type == PipelineType.REGISTRATION)
			return connection.getRegistration();
		if (type == PipelineType.MIGRATION)
			return connection.getMigration();

		return connection.getAuthentication();
	}

	private @NotNull JobKey jobKey(@NotNull PipelineType type, @NotNull PipelineStateReference reference) {
		String correlation = "pending:" + type.name() +
				"|c=" + reference.getConnectionUniqueId() +
				"|i=" + reference.getAccountUniqueId() +
				"|k=" + reference.getConnectionKey();

		return JobKey.of(ORIGIN, PURPOSE, correlation);
	}

	private boolean isPending(@NotNull PipelineState state) {
		return state.item(JourneyStateItem.class).isPresent();
	}
}
