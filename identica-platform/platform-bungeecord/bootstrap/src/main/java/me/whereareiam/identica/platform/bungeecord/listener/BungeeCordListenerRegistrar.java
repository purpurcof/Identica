package me.whereareiam.identica.platform.bungeecord.listener;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.CommonListenerRegistrar;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.platform.bungeecord.BungeeCordEventPriority;
import me.whereareiam.identica.platform.bungeecord.BungeeCordIdentica;
import me.whereareiam.identica.platform.bungeecord.listener.connection.BungeeCordDisconnectListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.BungeeCordLoginListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.BungeeCordPlayerHandshakeListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.server.BungeeCordServerConnectListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.server.BungeeCordServerConnectedListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.server.BungeeCordServerSwitchListener;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class BungeeCordListenerRegistrar extends CommonListenerRegistrar {
	private final Injector injector;
	private final BungeeCordIdentica plugin;
	private final DynamicListenerRegistry listenerRegistry;
	private final Map<Object, Listener> dynamicListeners = new ConcurrentHashMap<>();

	@Inject
	public BungeeCordListenerRegistrar(
			Injector injector,
			Provider<Settings> settingsProvider,
			BungeeCordIdentica plugin,
			DynamicListenerRegistry listenerRegistry
	) {
		super(settingsProvider);
		this.injector = injector;
		this.plugin = plugin;
		this.listenerRegistry = listenerRegistry;
	}

	@Override
	public void registerListeners() {
		listenerRegistry.attachRegistrar(this);

		registerListener(net.md_5.bungee.api.event.PlayerHandshakeEvent.class, injector.getInstance(BungeeCordPlayerHandshakeListener.class));
		registerListener(net.md_5.bungee.api.event.PostLoginEvent.class, injector.getInstance(BungeeCordLoginListener.class));
		registerListener(net.md_5.bungee.api.event.ServerConnectedEvent.class, injector.getInstance(BungeeCordServerConnectedListener.class));
		registerListener(net.md_5.bungee.api.event.ServerSwitchEvent.class, injector.getInstance(BungeeCordServerSwitchListener.class));
		registerListener(net.md_5.bungee.api.event.ServerConnectEvent.class, injector.getInstance(BungeeCordServerConnectListener.class));
		registerListener(net.md_5.bungee.api.event.PlayerDisconnectEvent.class, injector.getInstance(BungeeCordDisconnectListener.class));
	}

	@Override
	public <T> void registerListener(Class<T> eventClass, DynamicListener<T> listener) {
		if (shouldSkip(eventClass)) return;
		Logger.debug("Registering listener for event " + eventClass.getName());
		Listener bridge = dynamicListeners.computeIfAbsent(listener, key -> createBridge(eventClass, listener));
		plugin.getProxy().getPluginManager().registerListener(plugin, bridge);
	}

	private boolean shouldSkip(Class<?> eventClass) {
		var registration = settings
				.get()
				.getListeners()
				.getEvents()
				.get(eventClass.getName());

		return registration != null && !registration.isRegister();
	}

	private <T> Listener createBridge(Class<T> eventClass, DynamicListener<T> listener) {
		byte priority = BungeeCordEventPriority.of(determinePriority(eventClass));
		return new Listener() {
			@SuppressWarnings("unused")
			@EventHandler(priority = Byte.MAX_VALUE)
			public void on(Event event) {
				if (!eventClass.isInstance(event)) return;
				if (priority != Byte.MAX_VALUE) {
					// Bungee's EventHandler priority is compile-time, so dynamic priority remains best-effort only.
				}
				listener.onEvent(eventClass.cast(event));
			}
		};
	}
}
