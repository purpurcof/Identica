package me.whereareiam.identica.platform.velocity.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.EventManager;
import me.whereareiam.identica.common.CommonListenerRegistrar;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.platform.velocity.VelocityIdentica;
import me.whereareiam.identica.platform.velocity.util.VelocityUtil;

@Singleton
public class VelocityListenerRegistrar extends CommonListenerRegistrar {
	private final VelocityIdentica plugin;
	private final EventManager eventManager;

	@Inject
	public VelocityListenerRegistrar(
			Provider<Settings> settingsProvider,
			VelocityIdentica plugin,
			EventManager eventManager
	) {
		super(settingsProvider);
		this.plugin = plugin;
		this.eventManager = eventManager;
	}

	@Override
	public void registerListeners() {
	}

	@Override
	public <T> void registerListener(Class<T> eventClass, DynamicListener<T> listener) {
		if (settings.get().getListeners().getEvents().get(eventClass.getName()) != null
				&& !settings.get().getListeners().getEvents().get(eventClass.getName()).isRegister()) return;
		Logger.debug("Registering listener for event " + eventClass.getName());

		eventManager.register(plugin, eventClass, VelocityUtil.of(determinePriority(eventClass)), listener::onEvent);
	}
}
