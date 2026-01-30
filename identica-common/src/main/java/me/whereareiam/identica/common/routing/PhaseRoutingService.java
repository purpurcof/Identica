package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.routing.RoutingDecision;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.type.RoutingTargetType;
import me.whereareiam.identica.type.step.StepPhase;

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

		Settings.Routing routing = routing();
		if (routing == null)
			return Optional.empty();

		StepResult result = decision.getResult();
		if (result.getStatus() == null)
			return Optional.empty();

		String target = switch (result.getStatus()) {
			case COMPLETE -> routing.getTargets().getCompleted();
			case WAITING -> resolveStepTarget(routing, decision);
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
		String override = resolveOverride(routing.getOverrides(), decision.getStep() != null ? decision.getStep().getName() : null);
		if (!isBlank(override))
			return override;

		StepPhase phase = decision.getPhase();

		return switch (phase) {
			case PRE -> routing.getTargets().getPre();
			case PROVIDER -> routing.getTargets().getProvider();
			case END -> routing.getTargets().getEnd();
		};
	}

	private String resolveOverride(Settings.Routing.Overrides overrides, String stepName) {
		if (overrides == null || stepName == null)
			return null;

		for (Map.Entry<String, String> entry : overrides.getSteps().entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(stepName))
				return entry.getValue();
		}

		return null;
	}

	private Settings.Routing routing() {
		Settings settings = settingsProvider.get();
		return settings != null ? settings.getRouting() : null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
