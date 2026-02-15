package me.whereareiam.identica.common.identity.session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaReadyEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaShutdownEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.scheduler.JobKey;
import me.whereareiam.identica.model.scheduler.Origin;
import me.whereareiam.identica.model.scheduler.PeriodicalRunnableTask;
import me.whereareiam.identica.model.scheduler.Purpose;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.service.Scheduler;

import java.time.Duration;
@Singleton
public class SessionRefreshCoordinator implements EventListener {
	private static final Origin ORIGIN = Origin.core(SessionRefreshCoordinator.class);
	private static final Purpose PURPOSE = Purpose.of("session-refresh");

	private final Provider<Settings> settingsProvider;
	private final Scheduler scheduler;
	private final SessionService sessionService;
	private final IdentityService identityService;

	private PeriodicalRunnableTask task;

	@Inject
	public SessionRefreshCoordinator(
			Provider<Settings> settingsProvider,
			Scheduler scheduler,
			SessionService sessionService,
			IdentityService identityService,
			EventManager eventManager
	) {
		this.settingsProvider = settingsProvider;
		this.scheduler = scheduler;
		this.sessionService = sessionService;
		this.identityService = identityService;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onReady(IdenticaReadyEvent event) {
		onShutdown(null);

		Duration refreshInterval = settingsProvider.get().getConnection().getSessions().getRefreshTtl();
		if (!isUsable(refreshInterval)) return;
		if (scheduler == null) return;

		long intervalMs = refreshInterval.toMillis();
		task = PeriodicalRunnableTask.builder()
				.key(JobKey.of(ORIGIN, PURPOSE))
				.delay(intervalMs)
				.period(intervalMs)
				.runnable(this::refreshSessions)
				.build();
		scheduler.schedule(task);
	}

	@IdenticEvent
	public void onShutdown(IdenticaShutdownEvent event) {
		if (scheduler == null) return;
		if (task == null) return;
		scheduler.cancel(task.getKey());
		task = null;
	}

	private boolean isUsable(Duration duration) {
		return duration != null
				&& !duration.isZero()
				&& !duration.isNegative();
	}

	private void refreshSessions() {
		for (Identity identity : identityService.list()) {
			if (identity == null) continue;
			sessionService.refresh(identity.getUniqueId());
		}
	}
}
