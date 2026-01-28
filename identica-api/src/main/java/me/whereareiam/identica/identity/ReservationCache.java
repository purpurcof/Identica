package me.whereareiam.identica.identity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Cache for temporary identity reservations used during authentication.
 */
public interface ReservationCache {
	/**
	 * Retrieves a reserved UUID by key.
	 *
	 * @param key lookup key
	 * @return optional UUID result
	 */
	@NotNull CompletableFuture<Optional<UUID>> get(@Nullable String key);

	/**
	 * Stores a reserved UUID entry.
	 *
	 * @param key lookup key
	 * @param uuid UUID to store
	 * @param ttlMs time to live in milliseconds
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> put(@Nullable String key, @Nullable UUID uuid, long ttlMs);

	/**
	 * Invalidates a reservation entry.
	 *
	 * @param key lookup key
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> invalidate(@Nullable String key);
}
