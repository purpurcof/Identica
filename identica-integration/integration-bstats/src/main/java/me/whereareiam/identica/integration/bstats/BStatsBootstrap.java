package me.whereareiam.identica.integration.bstats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaReadyEvent;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;

@Singleton
public final class BStatsBootstrap implements EventListener {
	private final @NotNull TelemetryRegistrar telemetryRegistrar;
	private boolean initialized;

	@Inject
	public BStatsBootstrap(
			@NotNull TelemetryRegistrar telemetryRegistrar,
			@NotNull EventManager eventManager
	) {
		this.telemetryRegistrar = telemetryRegistrar;
		eventManager.register(this);
	}

	@IdenticEvent(EventOrder.NORMAL)
	public synchronized void onReady(@NotNull IdenticaReadyEvent event) {
		if (initialized) return;
		telemetryRegistrar.register();
		initialized = true;
	}
}
