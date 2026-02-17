package me.whereareiam.identica.common;

import com.google.inject.Inject;
import com.google.inject.Injector;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.common.logging.WelcomeBannerPrinter;
import me.whereareiam.identica.database.DatabaseService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaBootstrappedEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaReadyEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaShutdownEvent;
import me.whereareiam.identica.listener.ListenerRegistrar;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.logging.LoggingHelper;
import me.whereareiam.identica.model.config.*;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.ratelimit.RateLimitDefinition;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.type.event.EventOrder;
import me.whereareiam.identica.common.ratelimit.ResumeSpamRateLimitDefinition;

public class Identica implements EventListener {
	private final Injector injector;
	private final ListenerRegistrar listenerRegistrar;
	private final Registry<RateLimitDefinition> rateLimitRegistry;
	private final ResumeSpamRateLimitDefinition resumeSpamRateLimitDefinition;

	@Inject
	public Identica(
			Injector injector,
			EventManager eventManager,
			ListenerRegistrar listenerRegistrar,
			Registry<RateLimitDefinition> rateLimitRegistry,
			ResumeSpamRateLimitDefinition resumeSpamRateLimitDefinition
	) {
		this.injector = injector;
		this.listenerRegistrar = listenerRegistrar;
		this.rateLimitRegistry = rateLimitRegistry;
		this.resumeSpamRateLimitDefinition = resumeSpamRateLimitDefinition;

		eventManager.register(this);
	}

	@IdenticEvent
	public void onBootstrapped(IdenticaBootstrappedEvent event) {
		Logger.init(injector.getInstance(LoggingHelper.class));

		rateLimitRegistry.register(resumeSpamRateLimitDefinition);

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
		injector.getInstance(CommandService.class);
		injector.getInstance(ProviderManager.class).loadProviders();
		listenerRegistrar.registerListeners();

		injector.getInstance(WelcomeBannerPrinter.class).print();
	}

	@IdenticEvent(EventOrder.LOW)
	public void onShutdown(IdenticaShutdownEvent event) {
		injector.getInstance(ProviderManager.class).unloadProviders();

		rateLimitRegistry.unregister(resumeSpamRateLimitDefinition);

		IdenticaAPI.shutdown();
	}
}
