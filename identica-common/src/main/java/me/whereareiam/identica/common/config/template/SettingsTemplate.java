package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.event.EventPriority;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class SettingsTemplate implements TemplateProvider<Settings> {
	@Override
	public Settings supply(Settings settings) {
		Settings.Routing routing = new Settings.Routing();
		Settings.Routing.Defaults defaults = new Settings.Routing.Defaults();
		defaults.setCompletedTarget("lobby");
		defaults.setFallback("auth");
		routing.setDefaults(defaults);

		routing.setProviders(new HashMap<>());
		routing.getProviders().put("cracked", Map.of(
				"register", "register-1",
				"login", "auth-1"
		));

		Settings.Routing.Intent intent = new Settings.Routing.Intent();
		intent.setMode("allow");
		intent.setAllowedServers(new ArrayList<>());
		routing.setIntent(intent);
		settings.setRouting(routing);

		Settings.Listeners listeners = new Settings.Listeners();
		listeners.setEvents(defaultListenerEvents());
		settings.setListeners(listeners);

		Settings.Synchronization synchronization = new Settings.Synchronization();
		synchronization.setEnabled(false);
		synchronization.setServerId(UUID.randomUUID().toString());

		Settings.Synchronization.Redis redis = new Settings.Synchronization.Redis();
		redis.setHost("localhost");
		redis.setPort(6379);
		redis.setPassword("");
		redis.setSessionsIndexKey("identica:sessions:index");

		Settings.Synchronization.Channels channels = new Settings.Synchronization.Channels();
		channels.setAccountUpdates("identica:accounts");
		channels.setSessions("identica:sessions");
		channels.setConflicts("identica:conflicts");
		redis.setChannels(channels);
		synchronization.setRedis(redis);

		settings.setSynchronization(synchronization);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setDefaultTtlMinutes(Duration.ofHours(2));
		sessions.setRefreshMinutes(Duration.ofMinutes(10));
		sessions.setProviders(Map.of(
				"premium", Duration.ofHours(12)
		));
		settings.setSessions(sessions);

		Settings.Authentication authentication = new Settings.Authentication();
		authentication.setHandshakeInstructionTtlMinutes(Duration.ofMinutes(10));
		authentication.setPendingUuidTtlMinutes(Duration.ofMinutes(15));
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
