package me.whereareiam.identica.engine.pipeline.prompt;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.IdentityDetachedEvent;
import me.whereareiam.identica.event.pipeline.state.PipelineStateClearedEvent;
import me.whereareiam.identica.event.pipeline.state.PipelineStateSavedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentClearedEvent;
import me.whereareiam.identica.event.routing.step.StepRoutingReachedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.model.scheduler.JobKey;
import me.whereareiam.identica.model.scheduler.Origin;
import me.whereareiam.identica.model.scheduler.PeriodicalRunnableTask;
import me.whereareiam.identica.model.scheduler.Purpose;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.routing.RoutingAttemptService;
import me.whereareiam.identica.service.Scheduler;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.routing.reason.RoutingReason;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PendingPromptResendCoordinator implements EventListener {
	private static final Origin ORIGIN = Origin.core(PendingPromptResendCoordinator.class);
	private static final Purpose PURPOSE = Purpose.of("initial-step-prompt");

	private final RoutingAttemptService routingAttemptService;
	private final IdentityService identityService;
	private final Provider<Settings> settingsProvider;
	private final PipelineStateStore pipelineStateStore;
	private final Scheduler scheduler;

	private final Map<UUID, PendingPrompt> pendingPrompts = new ConcurrentHashMap<>();

	@Inject
	public PendingPromptResendCoordinator(
			@NotNull RoutingAttemptService routingAttemptService,
			@NotNull IdentityService identityService,
			@NotNull Provider<Settings> settingsProvider,
			@NotNull PipelineStateStore pipelineStateStore,
			@NotNull Scheduler scheduler,
			@NotNull EventManager eventManager
	) {
		this.routingAttemptService = routingAttemptService;
		this.identityService = identityService;
		this.settingsProvider = settingsProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.scheduler = scheduler;
		eventManager.register(this);
	}

	public boolean deferInitialStepPrompt(
			@NotNull UUID connectionUniqueId,
			ConnectionDecision decision
	) {
		if (decision == null || decision.getStatus() != ConnectionDecision.Status.WAIT) return false;

		String message = decision.getMessage();
		if (message == null || message.isBlank()) return false;

		RoutingIntent intent = routingAttemptService.current(connectionUniqueId).orElse(null);
		if (intent == null || intent.getReason() != RoutingReason.STEP) return false;

		PendingMarker marker = resolveCurrentMarker(connectionUniqueId);
		pendingPrompts.put(connectionUniqueId, new PendingPrompt(message, marker));
		Logger.debug(
				"Initial step prompt queued connection=%s target=%s step=%s marker=%s",
				connectionUniqueId,
				intent.getEndpoint().getServer(),
				intent.getStepName(),
				marker
		);
		return true;
	}

	public boolean hasPending(@NotNull UUID connectionUniqueId) {
		PendingPrompt prompt = pendingPrompts.get(connectionUniqueId);
		return prompt != null && !prompt.message().isBlank();
	}

	public boolean flush(@NotNull UUID connectionUniqueId, @NotNull String trigger) {
		return flush(connectionUniqueId, null, null, trigger);
	}

	public boolean flush(
			@NotNull UUID connectionUniqueId,
			String currentServer,
			String stepName,
			@NotNull String trigger
	) {
		PendingPrompt prompt = pendingPrompts.get(connectionUniqueId);
		if (prompt == null || prompt.message().isBlank())
			return false;

		Identity identity = identityService.find(connectionUniqueId).orElse(null);
		if (identity == null) {
			Logger.debug("Initial step prompt flush missed connection=%s trigger=%s reason=no-identity",
					connectionUniqueId, trigger);
			return false;
		}

		Component component = Serializer.serialize(identity, prompt.message());
		Logger.debug(
				"Initial step prompt flushed connection=%s trigger=%s current=%s step=%s plain=%s",
				connectionUniqueId,
				trigger,
				currentServer,
				stepName,
				PlainTextComponentSerializer.plainText().serialize(component)
		);
		identity.sendMessage(component);

		if (resendUntilInteractionEnabled()) {
			scheduleResend(connectionUniqueId);
			return true;
		}

		acknowledgeInteraction(connectionUniqueId, "single-delivery");
		return true;
	}

	public void acknowledgeInteraction(@NotNull UUID connectionUniqueId, @NotNull String reason) {
		PendingPrompt removed = pendingPrompts.remove(connectionUniqueId);
		scheduler.cancel(jobKey(connectionUniqueId));
		if (removed == null || removed.message().isBlank())
			return;

		Logger.debug("Initial step prompt acknowledged connection=%s reason=%s", connectionUniqueId, reason);
	}

	@IdenticEvent
	public void onStepRoutingReached(@NotNull StepRoutingReachedEvent event) {
		flush(
				event.getIntent().getConnectionUniqueId(),
				event.getCurrentServer(),
				event.getIntent().getStepName(),
				"step-routing-reached"
		);
	}

	@IdenticEvent
	public void onRoutingIntentCleared(@NotNull RoutingIntentClearedEvent event) {
		acknowledgeInteraction(event.getConnectionUniqueId(), "routing-intent-cleared");
	}

	@IdenticEvent
	public void onPipelineStateSaved(@NotNull PipelineStateSavedEvent event) {
		UUID connectionUniqueId = event.getReference().getConnectionUniqueId();
		if (connectionUniqueId == null)
			return;

		PendingPrompt prompt = pendingPrompts.get(connectionUniqueId);
		if (prompt == null)
			return;

		PendingMarker current = markerFrom(event.getState());
		if (prompt.marker().matches(current))
			return;

		acknowledgeInteraction(connectionUniqueId, "pipeline-state-changed");
	}

	@IdenticEvent
	public void onPipelineStateCleared(@NotNull PipelineStateClearedEvent event) {
		UUID connectionUniqueId = event.getReference().getConnectionUniqueId();
		if (connectionUniqueId == null)
			return;

		acknowledgeInteraction(connectionUniqueId, "pipeline-state-cleared");
	}

	@IdenticEvent
	public void onIdentityDetached(@NotNull IdentityDetachedEvent event) {
		acknowledgeInteraction(event.getUniqueId(), "identity-detached");
	}

	private void scheduleResend(@NotNull UUID connectionUniqueId) {
		long intervalMs = resendIntervalMillis();
		scheduler.schedule(PeriodicalRunnableTask.builder()
				.key(jobKey(connectionUniqueId))
				.delay(intervalMs)
				.period(intervalMs)
				.runnable(() -> resend(connectionUniqueId))
				.build());
	}

	private void resend(@NotNull UUID connectionUniqueId) {
		if (!hasPending(connectionUniqueId)) {
			scheduler.cancel(jobKey(connectionUniqueId));
			return;
		}

		Identity identity = identityService.find(connectionUniqueId).orElse(null);
		if (identity == null)
			return;

		PendingPrompt prompt = pendingPrompts.get(connectionUniqueId);
		if (prompt == null || prompt.message().isBlank()) {
			scheduler.cancel(jobKey(connectionUniqueId));
			return;
		}

		Component component = Serializer.serialize(identity, prompt.message());
		Logger.debug(
				"Initial step prompt resent connection=%s plain=%s",
				connectionUniqueId,
				PlainTextComponentSerializer.plainText().serialize(component)
		);
		identity.sendMessage(component);
	}

	private boolean resendUntilInteractionEnabled() {
		return settingsProvider.get().getConnection().getInitialPrompt().isResendUntilInteraction();
	}

	private long resendIntervalMillis() {
		return settingsProvider.get().getConnection().getInitialPrompt().resendIntervalMillis();
	}

	private @NotNull JobKey jobKey(@NotNull UUID connectionUniqueId) {
		return JobKey.of(ORIGIN, PURPOSE, connectionUniqueId.toString());
	}

	private @NotNull PendingMarker resolveCurrentMarker(@NotNull UUID connectionUniqueId) {
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.identityUniqueId(connectionUniqueId)
				.build();
		PipelineState state = pipelineStateStore.find(reference).orElse(null);
		return state != null ? markerFrom(state) : PendingMarker.empty();
	}

	private @NotNull PendingMarker markerFrom(@NotNull PipelineState state) {
		JourneyStateItem journey = state.item(JourneyStateItem.class).orElse(null);
		return new PendingMarker(
				state.getPipelineType(),
				journey != null ? journey.getStageId() : null,
				journey != null ? journey.getStepIndex() : -1
		);
	}

	private record PendingPrompt(
			@NotNull String message,
			@NotNull PendingMarker marker
	) {
	}

	private record PendingMarker(
			PipelineType pipelineType,
			String stageId,
			int stepIndex
	) {
		private static @NotNull PendingMarker empty() {
			return new PendingMarker(null, null, -1);
		}

		private boolean matches(@NotNull PendingMarker other) {
			return pipelineType == other.pipelineType
					&& stepIndex == other.stepIndex
					&& java.util.Objects.equals(stageId, other.stageId);
		}
	}
}
