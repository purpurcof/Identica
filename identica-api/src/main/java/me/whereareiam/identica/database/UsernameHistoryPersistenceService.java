package me.whereareiam.identica.database;

import me.whereareiam.identica.model.UsernameHistoryEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Service for recording username changes.
 */
public interface UsernameHistoryPersistenceService {
	/**
	 * Record a username change entry.
	 *
	 * @param entry username history entry to store
	 */
	void record(@NotNull UsernameHistoryEntry entry);
}
