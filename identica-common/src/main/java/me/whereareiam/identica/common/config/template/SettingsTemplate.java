package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.event.EventPriority;
import me.whereareiam.identica.type.pipeline.PipelineConcurrencyPolicy;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class SettingsTemplate implements TemplateProvider<Settings> {
	@Override
	public Settings supply(Settings settings) {
		settings.setLevel(2);

		Settings.Routing routing = new Settings.Routing();
		Settings.Routing.Targets targets = new Settings.Routing.Targets();
		targets.setStep("lobby");
		Settings.Routing.Targets.Overrides overrides = new Settings.Routing.Targets.Overrides();
		overrides.setStages(new HashMap<>(Map.of(
				"pre", "auth",
				"provider", "auth",
				"end", "lobby"
		)));
		overrides.setSteps(new HashMap<>());
		targets.setOverrides(overrides);
		routing.setTargets(targets);

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
		connection.setReservationTtl(Duration.ofMinutes(15));
		connection.setAuthentication(defaultConnectionScenario());
		connection.setRegistration(defaultConnectionScenario());
		settings.setConnection(connection);

		return settings;
	}

	private Settings.Scenario defaultConnectionScenario() {
		Settings.Scenario scenario = new Settings.Scenario();
		scenario.setPipelineTtl(Duration.ofMinutes(5));
		scenario.setAllowResume(true);
		scenario.setSessionConcurrencyPolicy(SessionConcurrencyPolicy.REPLACE_EXISTING);
		scenario.setPipelineConcurrencyPolicy(PipelineConcurrencyPolicy.DENY_NEW);
		scenario.setFlow(JourneyType.SEAMLESS);
		return scenario;
	}

	private Map<String, Event> defaultListenerEvents() {
		Map<String, Event> events = new HashMap<>();
		events.put("com.velocitypowered.api.event.connection.PreLoginEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.connection.LoginEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.player.GameProfileRequestEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.player.ServerPreConnectEvent", defaultEvent());
		events.put("com.velocitypowered.api.event.connection.DisconnectEvent", defaultEvent());

		return events;
	}

	private Event defaultEvent() {
		return Event.builder()
				.register(true)
				.priority(EventPriority.NORMAL)
				.build();
	}
}
