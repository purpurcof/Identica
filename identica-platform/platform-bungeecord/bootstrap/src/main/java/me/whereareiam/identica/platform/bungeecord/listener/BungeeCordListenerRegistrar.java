package me.whereareiam.identica.platform.bungeecord.listener;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.CommonListenerRegistrar;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.platform.bungeecord.BungeeCordIdentica;
import me.whereareiam.identica.platform.bungeecord.listener.connection.BungeeCordDisconnectListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.BungeeCordLoginListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.BungeeCordPlayerHandshakeListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.server.BungeeCordServerConnectListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.server.BungeeCordServerConnectedListener;
import me.whereareiam.identica.platform.bungeecord.listener.connection.server.BungeeCordServerSwitchListener;
import net.md_5.bungee.api.event.*;

@Singleton
public class BungeeCordListenerRegistrar extends CommonListenerRegistrar {
	private final Injector injector;
	private final DynamicListenerRegistry listenerRegistry;
	private final DynamicListenerRegistrar dynamicListenerRegistrar;

	@Inject
	public BungeeCordListenerRegistrar(
			Injector injector,
			Provider<Settings> settingsProvider,
			BungeeCordIdentica plugin,
			DynamicListenerRegistry listenerRegistry,
			DynamicListenerRegistrar dynamicListenerRegistrar
	) {
		super(settingsProvider);
		this.injector = injector;
		this.listenerRegistry = listenerRegistry;
		this.dynamicListenerRegistrar = dynamicListenerRegistrar;
	}

	@Override
	public void registerListeners() {
		listenerRegistry.attachRegistrar(this);

		registerListener(PlayerHandshakeEvent.class, injector.getInstance(BungeeCordPlayerHandshakeListener.class));
		registerListener(PostLoginEvent.class, injector.getInstance(BungeeCordLoginListener.class));
		registerListener(ServerConnectedEvent.class, injector.getInstance(BungeeCordServerConnectedListener.class));
		registerListener(ServerSwitchEvent.class, injector.getInstance(BungeeCordServerSwitchListener.class));
		registerListener(ServerConnectEvent.class, injector.getInstance(BungeeCordServerConnectListener.class));
		registerListener(PlayerDisconnectEvent.class, injector.getInstance(BungeeCordDisconnectListener.class));
	}

	@Override
	public <T> void registerListener(Class<T> eventClass, DynamicListener<T> listener) {
		dynamicListenerRegistrar.register(eventClass, listener);
	}
}
