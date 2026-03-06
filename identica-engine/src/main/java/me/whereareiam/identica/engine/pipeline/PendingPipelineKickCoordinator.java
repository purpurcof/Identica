package me.whereareiam.identica.engine.pipeline;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
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
import me.whereareiam.identica.model.scheduler.JobKey;
import me.whereareiam.identica.model.scheduler.Origin;
import me.whereareiam.identica.model.scheduler.Purpose;
import me.whereareiam.identica.model.scheduler.DelayedRunnableTask;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.service.Scheduler;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PendingPipelineKickCoordinator implements EventListener {
	private static final Origin ORIGIN = Origin.core(PendingPipelineKickCoordinator.class);
	private static final Purpose PURPOSE = Purpose.of("pipeline-expiry");

	private final Provider<Messages> messagesProvider;
	private final IdentityService identityService;
	private final Scheduler scheduler;
	private final EventManager eventManager;

	@Inject
	void registerListeners() {
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

		for (PipelineType candidate : PipelineType.values()) {
			cancelByKey(jobKey(candidate, reference));
		}
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

		String message = resolveExpiredMessage(type);
		identity.disconnect(Serializer.serialize(identity, message));
	}

	private @Nullable Identity resolveIdentity(@NotNull PipelineStateReference reference) {
		UUID connectionUniqueId = reference.getConnectionUniqueId();
		if (connectionUniqueId != null)
			return identityService.find(connectionUniqueId).orElse(null);

		UUID identityUniqueId = reference.getIdentityUniqueId();
		return identityUniqueId != null ? identityService.find(identityUniqueId).orElse(null) : null;
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
				"|i=" + reference.getIdentityUniqueId();

		return JobKey.of(ORIGIN, PURPOSE, correlation);
	}

	private boolean isPending(@NotNull PipelineState state) {
		return state.item(JourneyStateItem.class).isPresent();
	}
}
