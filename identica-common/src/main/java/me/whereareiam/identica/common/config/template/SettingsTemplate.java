package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.event.EventPriority;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class SettingsTemplate implements TemplateProvider<Settings> {
	@Override
	public Settings supply(Settings settings) {
		Settings.Routing routing = new Settings.Routing();
		routing.setDefaultTarget("lobby");
		routing.setFallback("auth");
		routing.setSteps(Map.of(
				"preLogin", "auth",
				"auth", "auth",
				"postAuth", "lobby"
		));
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

		Settings.Synchronization.Channels channels = new Settings.Synchronization.Channels();
		channels.setAccountUpdates("identica:accounts");
		channels.setSessions("identica:sessions");
		channels.setConflicts("identica:conflicts");
		redis.setChannels(channels);
		synchronization.setRedis(redis);

		settings.setSynchronization(synchronization);

		return settings;
	}

	private Map<String, Event> defaultListenerEvents() {
		Map<String, Event> events = new HashMap<>();
		Event event = Event.builder().register(true).priority(EventPriority.LOWEST).build();

		events.put("com.velocitypowered.api.event.connection.PostLoginEvent", event);
		events.put("com.velocitypowered.api.event.connection.DisconnectEvent", event);

		return events;
	}
}
