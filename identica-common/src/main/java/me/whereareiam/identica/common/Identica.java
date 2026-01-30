package me.whereareiam.identica.common;

import com.google.inject.Inject;
import com.google.inject.Injector;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.common.logging.WelcomeBannerPrinter;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.database.DatabaseService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaBootstrappedEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaReadyEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaShutdownEvent;
import me.whereareiam.identica.listener.ListenerRegistrar;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.event.EventOrder;

public class Identica implements EventListener {
	private final Injector injector;
	private final ListenerRegistrar listenerRegistrar;

	@Inject
	public Identica(
			Injector injector,
			EventManager eventManager,
			ListenerRegistrar listenerRegistrar
	) {
		this.injector = injector;
		this.listenerRegistrar = listenerRegistrar;

		eventManager.register(this);
	}

	@IdenticEvent
	public void onBootstrapped(IdenticaBootstrappedEvent event) {
		injector.getInstance(Settings.class);
		injector.getInstance(Messages.class);
		injector.getInstance(Commands.class);
		injector.getInstance(Providers.class);
		injector.getInstance(Replication.class);

		injector.getInstance(DatabaseService.class);

		IdenticaAPI.initialize(injector);
	}

	@IdenticEvent(EventOrder.LOW)
	public void onReady(IdenticaReadyEvent event) {
		listenerRegistrar.registerListeners();
		injector.getInstance(CommandService.class);
		injector.getInstance(ProviderManager.class).loadProviders();
		injector.getInstance(WelcomeBannerPrinter.class).print();
	}

	@IdenticEvent(EventOrder.LOW)
	public void onShutdown(IdenticaShutdownEvent event) {
		injector.getInstance(ProviderManager.class).unloadProviders();

		IdenticaAPI.shutdown();
	}
}
