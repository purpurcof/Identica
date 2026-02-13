package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.routing.RoutingDecision;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.type.RoutingTargetType;

import java.util.Map;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PhaseRoutingService implements RoutingService {
	private final Provider<Settings> settingsProvider;

	@Override
	public Optional<RoutingTarget> resolve(RoutingDecision decision) {
		if (decision == null || decision.getResult() == null)
			return Optional.empty();


		StepResult result = decision.getResult();
		Settings.Routing routing = settingsProvider.get().getConnection().getRouting();
		String target = switch (result.getStatus()) {
			case COMPLETE, WAITING -> resolveStepTarget(routing, decision);
			default -> null;
		};

		if (isBlank(target))
			return Optional.empty();

		RoutingTargetType type = result.getStatus() == StepResult.StepStatus.COMPLETE
				? RoutingTargetType.COMPLETED
				: RoutingTargetType.STEP;

		String providerId = decision.getContext().getProvider() != null
				? decision.getContext().getProvider().getProviderId()
				: null;
		String stepName = decision.getStep() != null ? decision.getStep().getName() : null;

		return Optional.of(new RoutingTarget(type, target.trim(), providerId, stepName));
	}

	private String resolveStepTarget(Settings.Routing routing, RoutingDecision decision) {
		String target = routing.getTargets().getStep();

		Settings.Routing.Targets.Overrides overrides = routing.getTargets().getOverrides();
		String stageOverride = resolveStageOverride(overrides, decision.getPhase().id());
		if (!isBlank(stageOverride))
			target = stageOverride;

		String override = resolveOverride(overrides, decision.getStep() != null ? decision.getStep().getName() : null);
		if (!isBlank(override))
			target = override;

		return target;
	}

	private String resolveOverride(Settings.Routing.Targets.Overrides overrides, String stepName) {
		if (overrides == null || stepName == null)
			return null;

		for (Map.Entry<String, String> entry : overrides.getSteps().entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(stepName))
				return entry.getValue();
		}

		return null;
	}

	private String resolveStageOverride(Settings.Routing.Targets.Overrides overrides, String stageId) {
		if (overrides == null || stageId == null)
			return null;

		for (Map.Entry<String, String> entry : overrides.getStages().entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(stageId))
				return entry.getValue();
		}

		return null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
