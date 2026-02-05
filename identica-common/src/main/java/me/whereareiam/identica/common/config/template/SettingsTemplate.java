package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.event.EventPriority;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import me.whereareiam.identica.type.step.AuthFlowType;

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
		targets.setPre("auth");
		targets.setProvider("auth");
		targets.setEnd("lobby");
		targets.setCompleted("lobby");
		routing.setTargets(targets);

		Settings.Routing.Overrides overrides = new Settings.Routing.Overrides();
		overrides.setSteps(new HashMap<>());
		routing.setOverrides(overrides);
		settings.setRouting(routing);

		Settings.Listeners listeners = new Settings.Listeners();
		listeners.setEvents(defaultListenerEvents());
		settings.setListeners(listeners);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setDefaultTtl(Duration.ofHours(2));
		sessions.setRefreshTtl(Duration.ofMinutes(10));
		sessions.setProviders(Map.of(
				"premium", Duration.ofHours(12)
		));
		sessions.setConcurrencyPolicy(SessionConcurrencyPolicy.KICK_EXISTING);
		sessions.setConcurrencyOverrides(new HashMap<>());
		settings.setSessions(sessions);

		Settings.Authentication authentication = new Settings.Authentication();
		authentication.setHandshakeInstructionTtl(Duration.ofMinutes(10));
		authentication.setPendingTtl(Duration.ofMinutes(5));
		authentication.setReservationTtl(Duration.ofMinutes(15));
		authentication.setFlow(AuthFlowType.SEAMLESS);
		settings.setAuthentication(authentication);

		return settings;
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
