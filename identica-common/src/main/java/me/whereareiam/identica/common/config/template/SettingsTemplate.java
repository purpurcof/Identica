package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import me.whereareiam.identica.type.event.EventPriority;
import me.whereareiam.identica.type.identity.UniqueIdMode;
import me.whereareiam.identica.type.pipeline.PipelineConcurrencyPolicy;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.JourneyPolicy;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class SettingsTemplate implements TemplateProvider<Settings> {
	@Override
	public Settings supply(Settings settings) {
		settings.setLevel(2);

		Settings.Routing routing = new Settings.Routing();
		routing.setDefaults(defaultRoutingDefaults());
		routing.setScenarios(defaultScenarioRoutingTargets());

		Settings.Listeners listeners = new Settings.Listeners();
		listeners.setEvents(defaultListenerEvents());
		settings.setListeners(listeners);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setDefaultTtl(Duration.ofHours(2));
		sessions.setRefreshTtl(Duration.ofMinutes(10));
		sessions.setProviders(Map.of(
				"premium", Duration.ofHours(12)
		));
		sessions.setConcurrencyPolicy(SessionConcurrencyPolicy.REPLACE_EXISTING);
		sessions.setConcurrencyOverrides(new HashMap<>());

		Settings.Connection connection = new Settings.Connection();
		connection.setRouting(routing);
		connection.setSessions(sessions);
		connection.setHandshakeInstructionTtl(Duration.ofMinutes(10));
		connection.setAttemptTtl(Duration.ofMinutes(10));
		connection.setReservationTtl(Duration.ofMinutes(15));
		connection.setPrepareStateTtl(Duration.ofMinutes(10));
		connection.setUniqueIdMode(UniqueIdMode.RANDOM);
		connection.setAuthentication(defaultAuthenticationScenario());
		connection.setRegistration(defaultRegistrationScenario());
		connection.setMigration(defaultMigrationScenario());
		connection.setSentinels(defaultSentinels());
		settings.setConnection(connection);

		return settings;
	}

	private Settings.AuthenticationScenario defaultAuthenticationScenario() {
		Settings.AuthenticationScenario scenario = new Settings.AuthenticationScenario();
		scenario.setPipelineTtl(Duration.ofMinutes(5));
		scenario.setAdvanceLockTtl(Duration.ofSeconds(5));
		scenario.setAllowResume(true);
		scenario.setSessionConcurrencyPolicy(SessionConcurrencyPolicy.REPLACE_EXISTING);
		scenario.setPipelineConcurrencyPolicy(PipelineConcurrencyPolicy.DENY_NEW);
		scenario.setJourneyMode(JourneyMode.SEAMLESS);
		scenario.setJourneyPolicy(JourneyPolicy.PREFER);
		return scenario;
	}

	private Settings.RegistrationScenario defaultRegistrationScenario() {
		Settings.RegistrationScenario scenario = new Settings.RegistrationScenario();
		scenario.setPipelineTtl(Duration.ofMinutes(5));
		scenario.setAdvanceLockTtl(Duration.ofSeconds(5));
		scenario.setAllowResume(true);
		scenario.setPipelineConcurrencyPolicy(PipelineConcurrencyPolicy.DENY_NEW);
		scenario.setAutoSelectSingleProvider(false);
		scenario.setJourneyMode(JourneyMode.SEAMLESS);
		scenario.setJourneyPolicy(JourneyPolicy.PREFER);
		return scenario;
	}

	private Settings.MigrationScenario defaultMigrationScenario() {
		Settings.MigrationScenario scenario = new Settings.MigrationScenario();
		scenario.setPipelineTtl(Duration.ofMinutes(5));
		scenario.setAdvanceLockTtl(Duration.ofSeconds(5));
		scenario.setAllowResume(true);
		scenario.setJourneyMode(JourneyMode.INTERACTIVE);
		scenario.setJourneyPolicy(JourneyPolicy.PREFER);
		return scenario;
	}

	private Settings.Sentinels defaultSentinels() {
		Settings.Sentinels sentinels = new Settings.Sentinels();
		SentinelPolicy resumeSpam = new SentinelPolicy();
		resumeSpam.setEnabled(false);
		resumeSpam.setMaxAttempts(10);

		SentinelPolicy.Lockout lockout = new SentinelPolicy.Lockout();
		lockout.setEnabled(true);
		lockout.setDuration(Duration.ofSeconds(30));
		resumeSpam.setLockout(lockout);

		SentinelPolicy.Warning warning = new SentinelPolicy.Warning();
		warning.setEnabled(false);
		warning.setThresholdPercentage(0);
		resumeSpam.setWarning(warning);
		sentinels.setResumeSpam(resumeSpam);

		return sentinels;
	}

	private Map<String, Settings.Routing.Targets> defaultScenarioRoutingTargets() {
		Map<String, Settings.Routing.Targets> scenarios = new HashMap<>();
		scenarios.put("authentication", createScenarioTargets("auth", "survival"));
		scenarios.put("registration", createScenarioTargets("register", "tutorial"));
		scenarios.put("migration", createScenarioTargets("migrate", "survival"));
		return scenarios;
	}

	private Settings.Routing.Defaults defaultRoutingDefaults() {
		Settings.Routing.Defaults defaults = new Settings.Routing.Defaults();

		Settings.Routing.Target step = Settings.Routing.Target.step();
		step.setTarget("auth");
		step.setAttempts(RoutingAttemptPolicy.defaultStep());

		Settings.Routing.Target complete = Settings.Routing.Target.complete();
		complete.setTarget("survival");
		complete.setAttempts(RoutingAttemptPolicy.defaultCompletion());

		defaults.setStep(step);
		defaults.setComplete(complete);
		return defaults;
	}

	private Settings.Routing.Targets createScenarioTargets(String step, String complete) {
		Settings.Routing.Targets targets = new Settings.Routing.Targets();
		Settings.Routing.Target stepTarget = Settings.Routing.Target.step();
		stepTarget.setTarget(step);
		Settings.Routing.Target completeTarget = Settings.Routing.Target.complete();
		completeTarget.setTarget(complete);

		targets.setStep(stepTarget);
		targets.setComplete(completeTarget);

		Settings.Routing.Targets.Overrides overrides = new Settings.Routing.Targets.Overrides();
		overrides.setStages(new HashMap<>());
		overrides.setSteps(new HashMap<>());
		targets.setOverrides(overrides);
		return targets;
	}

	private Map<String, Event> defaultListenerEvents() {
		Map<String, Event> events = new HashMap<>();
		events.put("com.velocitypowered.api.event.connection.PreLoginEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.connection.LoginEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.player.GameProfileRequestEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent", defaultEvent(EventPriority.HIGH));
		events.put("com.velocitypowered.api.event.player.ServerPreConnectEvent", defaultEvent(EventPriority.HIGH));
		events.put("com.velocitypowered.api.event.connection.DisconnectEvent", defaultEvent());

		return events;
	}

	private Event defaultEvent() {
		return defaultEvent(EventPriority.NORMAL);
	}

	private Event defaultEvent(EventPriority priority) {
		return Event.builder()
				.register(true)
				.priority(priority)
				.build();
	}
}
