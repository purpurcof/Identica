package me.whereareiam.identica.database;

import me.whereareiam.identica.model.UsernameHistoryEntry;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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

	/**
	 * Delete all username history for an account.
	 *
	 * @param uniqueId account id
	 */
	void deleteAll(@NotNull UUID uniqueId);
}
