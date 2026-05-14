package me.whereareiam.identica.common.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import me.whereareiam.identica.type.PlatformType;
import me.whereareiam.identica.type.event.EventPriority;
import me.whereareiam.identica.type.identity.UniqueIdMode;
import me.whereareiam.identica.type.pipeline.PipelineConcurrencyPolicy;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.JourneyPolicy;
import me.whereareiam.identica.type.session.RecognitionSignal;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SettingsDefaults implements MergeDefaultsProvider<Settings> {
	@Override
	public Settings supply(Settings settings) {
		settings.setLevel(2);

		Settings.Routing routing = new Settings.Routing();
		routing.setDefaults(defaultRoutingDefaults());
		routing.setScenarios(new HashMap<>());

		Settings.Listeners listeners = new Settings.Listeners();
		listeners.setEvents(defaultListenerEvents());
		settings.setListeners(listeners);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setConcurrencyPolicy(SessionConcurrencyPolicy.REPLACE_EXISTING);
		sessions.setActiveTtl(Duration.ofHours(12));
		Settings.Sessions.Recognition recognition = new Settings.Sessions.Recognition();
		recognition.setEnabled(false);
		recognition.setValidity(Duration.ofHours(12));
		recognition.setDefaultSignals(java.util.List.of(
				RecognitionSignal.USERNAME,
				RecognitionSignal.IP,
				RecognitionSignal.VIRTUAL_HOST
		));
		Settings.Sessions.Recognition.UntrustedIps untrustedIps = new Settings.Sessions.Recognition.UntrustedIps();
		untrustedIps.setEnabled(true);
		untrustedIps.setEntries(List.of(
				"127.0.0.1",
				"::1",
				"10.0.0.0/8",
				"172.16.0.0/12",
				"192.168.0.0/16"
		));
		recognition.setUntrustedIps(untrustedIps);
		sessions.setRecognition(recognition);

		Settings.InitialPrompt initialPrompt = new Settings.InitialPrompt();
		initialPrompt.setResendUntilInteraction(false);
		initialPrompt.setResendInterval(Duration.ofMillis(1500));

		Settings.Connection connection = new Settings.Connection();
		connection.setRouting(routing);
		connection.setSessions(sessions);
		connection.setInitialPrompt(initialPrompt);
		connection.setHandshakeInstructionTtl(Duration.ofMinutes(10));
		connection.setAttemptTtl(Duration.ofMinutes(10));
		connection.setReservationTtl(Duration.ofMinutes(15));
		connection.setPrepareStateTtl(Duration.ofMinutes(10));
		connection.setUniqueIdMode(UniqueIdMode.RANDOM);
		Settings.Scenarios scenarios = new Settings.Scenarios();
		scenarios.setAuthentication(defaultAuthenticationScenario());
		scenarios.setRegistration(defaultRegistrationScenario());
		scenarios.setMigration(defaultMigrationScenario());
		connection.setScenarios(scenarios);
		connection.setSentinels(defaultSentinels());
		settings.setConnection(connection);

		return settings;
	}

	private Settings.AuthenticationScenario defaultAuthenticationScenario() {
		Settings.AuthenticationScenario scenario = new Settings.AuthenticationScenario();
		scenario.setPipelineTtl(Duration.ofMinutes(5));
		scenario.setAdvanceLockTtl(Duration.ofSeconds(5));
		scenario.setAllowResume(true);
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

	private Map<String, Event> defaultListenerEvents() {
		return defaultListenerEvents(PlatformType.getType());
	}

	Map<String, Event> defaultListenerEvents(PlatformType platformType) {
		return switch (platformType) {
			case BUNGEECORD -> defaultBungeeCordListenerEvents();
			case VELOCITY, UNKNOWN -> defaultVelocityListenerEvents();
		};
	}

	private Map<String, Event> defaultVelocityListenerEvents() {
		Map<String, Event> events = new LinkedHashMap<>();
		events.put("com.velocitypowered.api.event.connection.PreLoginEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.player.GameProfileRequestEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.connection.LoginEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent", defaultEvent(EventPriority.HIGH));
		events.put("com.velocitypowered.api.event.player.ServerPreConnectEvent", defaultEvent(EventPriority.HIGH));
		events.put("com.velocitypowered.api.event.player.ServerPostConnectEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.connection.DisconnectEvent", defaultEvent());
		return events;
	}

	private Map<String, Event> defaultBungeeCordListenerEvents() {
		Map<String, Event> events = new LinkedHashMap<>();
		events.put("net.md_5.bungee.api.event.PlayerHandshakeEvent", defaultEvent());
		events.put("net.md_5.bungee.api.event.LoginEvent", defaultEvent());
		events.put("net.md_5.bungee.api.event.PostLoginEvent", defaultEvent());
		events.put("net.md_5.bungee.api.event.ServerConnectEvent", defaultEvent(EventPriority.HIGH));
		events.put("net.md_5.bungee.api.event.ServerConnectedEvent", defaultEvent(EventPriority.HIGH));
		events.put("net.md_5.bungee.api.event.ServerSwitchEvent", defaultEvent());
		events.put("net.md_5.bungee.api.event.PlayerDisconnectEvent", defaultEvent());

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
