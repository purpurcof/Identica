package me.whereareiam.identica.common.identity.session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaReadyEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaShutdownEvent;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.identity.session.SessionRefreshApplier;

import java.time.Duration;
@Singleton
public class SessionRefreshCoordinator implements EventListener {
	private final Provider<Settings> settingsProvider;
	private final SessionRefreshApplier applier;

	@Inject
	public SessionRefreshCoordinator(
			Provider<Settings> settingsProvider,
			SessionRefreshApplier applier,
			EventManager eventManager
	) {
		this.settingsProvider = settingsProvider;
		this.applier = applier;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onReady(IdenticaReadyEvent event) {
		onShutdown(null);

		Duration refreshInterval = settingsProvider.get().getConnection().getSessions().getRefreshTtl();
		if (!isUsable(refreshInterval)) return;
		if (applier == null) return;
		applier.start(refreshInterval);
	}

	@IdenticEvent
	public void onShutdown(IdenticaShutdownEvent event) {
		if (applier == null) return;
		applier.stop();
	}

	private boolean isUsable(Duration duration) {
		return duration != null
				&& !duration.isZero()
				&& !duration.isNegative();
	}
}
