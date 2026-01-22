package me.whereareiam.identica.common.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.type.RoutingTargetType;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.type.AuthStepType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultRoutingService implements RoutingService {
	private final Provider<Settings> settingsProvider;

	@Override
	public Optional<RoutingTarget> resolveStepTarget(
			AuthContext context,
			String providerId,
			AuthenticationStep step
	) {
		if (context == null || providerId == null || step == null) return Optional.empty();
		if (step.getType() != AuthStepType.INTERACTIVE) return Optional.empty();

		Settings.Routing routing = routing();
		if (routing == null) return Optional.empty();

		String target = resolveStepServer(routing.getProviders(), providerId, step.getName());
		if (isBlank(target)) {
			target = routing.getDefaults() != null ? routing.getDefaults().getFallback() : null;
		}

		if (isBlank(target)) return Optional.empty();
		return Optional.of(new RoutingTarget(RoutingTargetType.STEP, target.trim(), providerId, step.getName()));
	}

	@Override
	public Optional<RoutingTarget> resolveCompletionTarget(AuthContext context) {
		if (context == null) return Optional.empty();

		Settings.Routing routing = routing();
		if (routing == null || routing.getDefaults() == null) return Optional.empty();

		String finalTarget = routing.getDefaults().getCompletedTarget();

		Settings.Routing.Intent intent = routing.getIntent();
		String mode = intent != null ? intent.getMode() : null;
		String intendedServer = context.getIntendedServer();

		if ("allow".equalsIgnoreCase(mode)
				&& !isBlank(intendedServer)
				&& isAllowed(intent.getAllowedServers(), intendedServer)) {
			finalTarget = intendedServer;
		}

		if (isBlank(finalTarget)) return Optional.empty();
		return Optional.of(new RoutingTarget(RoutingTargetType.COMPLETED, finalTarget.trim(), null, null));
	}

	private Settings.Routing routing() {
		Settings settings = settingsProvider.get();
		return settings != null ? settings.getRouting() : null;
	}

	private boolean isAllowed(List<String> allowedServers, String intendedServer) {
		if (allowedServers == null || allowedServers.isEmpty()) return true;

		for (String allowed : allowedServers)
			if (allowed != null && allowed.equalsIgnoreCase(intendedServer))
				return true;

		return false;
	}

	private String resolveStepServer(
			Map<String, Map<String, String>> providers,
			String providerId,
			String stepName
	) {
		if (providers == null || providers.isEmpty() || isBlank(providerId) || isBlank(stepName)) return null;

		Map<String, String> steps = findProviderSteps(providers, providerId);
		if (steps == null || steps.isEmpty()) return null;

		for (Map.Entry<String, String> entry : steps.entrySet())
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(stepName))
				return entry.getValue();

		return null;
	}

	private Map<String, String> findProviderSteps(
			Map<String, Map<String, String>> providers,
			String providerId
	) {
		for (Map.Entry<String, Map<String, String>> entry : providers.entrySet())
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(providerId))
				return entry.getValue();

		return null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
