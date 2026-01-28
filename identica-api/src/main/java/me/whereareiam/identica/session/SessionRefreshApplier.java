package me.whereareiam.identica.session;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/**
 * Platform adapter responsible for refreshing sessions on a schedule.
 */
@RequiredArgsConstructor
public abstract class SessionRefreshApplier {
	private final SessionService sessionService;

	/**
	 * Starts the refresh scheduler with the provided interval.
	 *
	 * @param refreshInterval refresh interval
	 */
	public final void start(@Nullable Duration refreshInterval) {
		stop();
		if (!isUsable(refreshInterval)) return;
		schedule(refreshInterval, this::refreshSessions);
	}

	/**
	 * Stops the refresh scheduler.
	 */
	public final void stop() {
		cancelSchedule();
	}

	/**
	 * Schedules a refresh task.
	 *
	 * @param refreshInterval refresh interval
	 * @param task task to execute
	 */
	protected abstract void schedule(@NotNull Duration refreshInterval, @NotNull Runnable task);

	/**
	 * Cancels scheduled refresh tasks.
	 */
	protected abstract void cancelSchedule();

	/**
	 * Returns the set of online identity ids.
	 *
	 * @return collection of online ids
	 */
	protected abstract @Nullable Collection<UUID> getOnlineUniqueIds();

	private void refreshSessions() {
		Collection<UUID> online = getOnlineUniqueIds();
		if (online == null || online.isEmpty()) return;

		for (UUID uniqueId : online) {
			if (uniqueId == null) continue;
			sessionService.refresh(uniqueId);
		}
	}

	private boolean isUsable(Duration duration) {
		return duration != null
				&& !duration.isZero()
				&& !duration.isNegative();
	}
}
