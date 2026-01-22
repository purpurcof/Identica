package me.whereareiam.identica.common.routing;

import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.type.RoutingTargetType;
import me.whereareiam.identica.routing.RoutingService;
import me.whereareiam.identica.type.AuthStepType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRoutingServiceTest {
	@Test
	void resolvesStepTargetFromProviderConfig() {
		Settings settings = baseSettings();
		settings.getRouting().getProviders().put("Cracked", Map.of("register", "register-1"));

		RoutingService service = new DefaultRoutingService(() -> settings);
		AuthenticationStep step = new TestStep("REGISTER", AuthStepType.INTERACTIVE);
		AuthContext context = context();

		Optional<RoutingTarget> target = service.resolveStepTarget(context, "cracked", step);

		assertTrue(target.isPresent());
		assertEquals(RoutingTargetType.STEP, target.get().getType());
		assertEquals("register-1", target.get().getServer());
	}

	@Test
	void fallsBackWhenStepMissing() {
		Settings settings = baseSettings();
		settings.getRouting().getDefaults().setFallback("auth");

		RoutingService service = new DefaultRoutingService(() -> settings);
		AuthenticationStep step = new TestStep("login", AuthStepType.INTERACTIVE);
		AuthContext context = context();

		Optional<RoutingTarget> target = service.resolveStepTarget(context, "cracked", step);

		assertTrue(target.isPresent());
		assertEquals("auth", target.get().getServer());
	}

	@Test
	void allowsIntentWhenModeAllowAndWhitelistEmpty() {
		Settings settings = baseSettings();
		settings.getRouting().getDefaults().setCompletedTarget("lobby");
		settings.getRouting().getIntent().setMode("allow");
		settings.getRouting().getIntent().setAllowedServers(List.of());

		RoutingService service = new DefaultRoutingService(() -> settings);
		AuthContext context = context().toBuilder()
				.intendedServer("survival")
				.build();

		Optional<RoutingTarget> target = service.resolveCompletionTarget(context);

		assertTrue(target.isPresent());
		assertEquals(RoutingTargetType.COMPLETED, target.get().getType());
		assertEquals("survival", target.get().getServer());
	}

	@Test
	void ignoresIntentWhenModeIgnore() {
		Settings settings = baseSettings();
		settings.getRouting().getDefaults().setCompletedTarget("lobby");
		settings.getRouting().getIntent().setMode("ignore");

		RoutingService service = new DefaultRoutingService(() -> settings);
		AuthContext context = context().toBuilder()
				.intendedServer("survival")
				.build();

		Optional<RoutingTarget> target = service.resolveCompletionTarget(context);

		assertTrue(target.isPresent());
		assertEquals("lobby", target.get().getServer());
	}

	private static Settings baseSettings() {
		Settings settings = new Settings();
		Settings.Routing routing = new Settings.Routing();

		Settings.Routing.Defaults defaults = new Settings.Routing.Defaults();
		defaults.setCompletedTarget("lobby");
		defaults.setFallback("auth");
		routing.setDefaults(defaults);
		routing.setProviders(new HashMap<>());

		Settings.Routing.Intent intent = new Settings.Routing.Intent();
		intent.setMode("allow");
		intent.setAllowedServers(List.of());
		routing.setIntent(intent);
		settings.setRouting(routing);

		return settings;
	}

	private static AuthContext context() {
		return AuthContext.builder()
				.connectionUniqueId(UUID.randomUUID())
				.username("Steve")
				.ip("127.0.0.1")
				.intendedServer("lobby")
				.build();
	}

	private record TestStep(String name, AuthStepType type) implements AuthenticationStep {
		@Override
		public String getName() {
			return name;
		}

		@Override
		public AuthStepType getType() {
			return type;
		}

		@Override
		public CompletableFuture<StepResult> execute(
				AuthContext context
		) {
			return CompletableFuture.completedFuture(null);
		}
	}
}
