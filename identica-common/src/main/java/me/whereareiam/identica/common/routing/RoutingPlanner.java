package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptState;
import me.whereareiam.identica.model.routing.RoutingEndpoint;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.model.routing.RoutingPlan;
import me.whereareiam.identica.model.routing.RoutingSignal;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.routing.RoutingClearReason;
import me.whereareiam.identica.type.routing.RoutingReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RoutingPlanner {
	private final Provider<Settings> settingsProvider;

	public @NotNull RoutingPlan plan(@NotNull RoutingSignal signal) {
		UUID connectionId = signal.connectionUniqueId();
		if (connectionId == null) return RoutingPlan.ignore();

		return switch (signal.getType()) {
			case STEP_FINISHED -> planStep(signal, connectionId);
			case PIPELINE_FINISHED -> planPipeline(signal, connectionId);
		};
	}

	private @NotNull RoutingPlan planStep(@NotNull RoutingSignal signal, @NotNull UUID connectionId) {
		StepResult result = signal.getStepResult();
		if (result == null) return RoutingPlan.ignore();
		if (result.getStatus() == StepResult.StepStatus.COMPLETE) return RoutingPlan.ignore();
		if (result.getStatus() != StepResult.StepStatus.WAITING)
			return RoutingPlan.clear(connectionId, RoutingClearReason.PIPELINE_FAILED);

		Settings.Routing.Target target = resolveStepTarget(signal);
		if (target == null || isBlank(target.getTarget()))
			return RoutingPlan.clear(connectionId, RoutingClearReason.NO_TARGET);

		return RoutingPlan.replace(createIntent(signal, connectionId, target, RoutingReason.STEP));
	}

	private @NotNull RoutingPlan planPipeline(@NotNull RoutingSignal signal, @NotNull UUID connectionId) {
		PipelineResult result = signal.getPipelineResult();
		if (result == null) return RoutingPlan.ignore();
		if (result.getStatus() == PipelineStatus.WAITING) return RoutingPlan.ignore();
		if (result.getStatus() != PipelineStatus.COMPLETE)
			return RoutingPlan.clear(connectionId, RoutingClearReason.PIPELINE_FAILED);

		Settings.Routing.Target target = resolveCompletionTarget(signal);
		if (target == null || isBlank(target.getTarget()))
			return RoutingPlan.clear(connectionId, RoutingClearReason.NO_TARGET);

		return RoutingPlan.replace(createIntent(signal, connectionId, target, RoutingReason.COMPLETION));
	}

	private @NotNull RoutingIntent createIntent(
			@NotNull RoutingSignal signal,
			@NotNull UUID connectionId,
			@NotNull Settings.Routing.Target target,
			@NotNull RoutingReason reason
	) {
		String providerId = signal.getContext().getProvider() != null
				? signal.getContext().getProvider().getProviderId()
				: null;
		String stepName = signal.getStep() != null ? signal.getStep().getName() : null;
		long now = System.currentTimeMillis();
		return new RoutingIntent(
				UUID.randomUUID(),
				connectionId,
				new RoutingEndpoint(target.getTarget().trim()),
				reason,
				copyPolicy(target.getAttempts(), reason),
				new RoutingAttemptState(),
				signal.getPipelineType(),
				signal.getStage(),
				providerId,
				stepName,
				now
		);
	}

	private Settings.Routing.Target resolveCompletionTarget(@NotNull RoutingSignal signal) {
		Settings.Routing routing = settingsProvider.get().getConnection().getRouting();
		Settings.Routing.Targets scenario = resolveScenarioTargets(routing, signal.getPipelineType());
		Settings.Routing.Target target = scenario != null ? scenario.getComplete() : null;
		if (target != null && !isBlank(target.getTarget())) return target;
		return routing.getDefaults().getComplete();
	}

	private Settings.Routing.Target resolveStepTarget(@NotNull RoutingSignal signal) {
		Settings.Routing routing = settingsProvider.get().getConnection().getRouting();
		Settings.Routing.Targets scenario = resolveScenarioTargets(routing, signal.getPipelineType());
		Settings.Routing.Target target = scenario != null ? scenario.getStep() : null;
		if (target == null || isBlank(target.getTarget()))
			target = routing.getDefaults().getStep();

		Settings.Routing.Targets.Overrides overrides = scenario != null ? scenario.getOverrides() : null;
		Settings.Routing.Target stageOverride = resolveStageOverride(overrides, signal.getStage() != null ? signal.getStage().id() : null);
		if (stageOverride != null && !isBlank(stageOverride.getTarget()))
			target = stageOverride;

		Settings.Routing.Target stepOverride = resolveStepOverride(overrides, signal.getStep() != null ? signal.getStep().getName() : null);
		if (stepOverride != null && !isBlank(stepOverride.getTarget()))
			target = stepOverride;

		return target;
	}

	private Settings.Routing.Targets resolveScenarioTargets(Settings.Routing routing, PipelineType pipelineType) {
		if (routing == null || pipelineType == null) return null;

		String scenarioId = pipelineType.name().toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Settings.Routing.Targets> entry : routing.getScenarios().entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) continue;
			if (entry.getKey().equalsIgnoreCase(scenarioId))
				return entry.getValue();
		}

		return null;
	}

	private Settings.Routing.Target resolveStageOverride(@Nullable Settings.Routing.Targets.Overrides overrides, @Nullable String stageId) {
		if (overrides == null || stageId == null) return null;
		return resolveOverride(overrides.getStages(), stageId);
	}

	private Settings.Routing.Target resolveStepOverride(@Nullable Settings.Routing.Targets.Overrides overrides, @Nullable String stepName) {
		if (overrides == null || stepName == null) return null;
		return resolveOverride(overrides.getSteps(), stepName);
	}

	private Settings.Routing.Target resolveOverride(Map<String, Settings.Routing.Target> overrides, String key) {
		for (Map.Entry<String, Settings.Routing.Target> entry : overrides.entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key))
				return entry.getValue();
		}
		return null;
	}

	private @NotNull RoutingAttemptPolicy copyPolicy(@Nullable RoutingAttemptPolicy source, @NotNull RoutingReason reason) {
		RoutingAttemptPolicy fallback = reason == RoutingReason.COMPLETION
				? RoutingAttemptPolicy.defaultCompletion()
				: RoutingAttemptPolicy.defaultStep();
		if (source == null) return fallback;

		RoutingAttemptPolicy copy = new RoutingAttemptPolicy();
		copy.setMode(source.getMode());
		copy.setMaxAttempts(source.getMaxAttempts());
		copy.setRetryDelay(source.getRetryDelay());
		copy.setConsumeOnReached(source.isConsumeOnReached());
		copy.setConsumeOnExhausted(source.isConsumeOnExhausted());
		return copy;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
