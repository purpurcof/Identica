package me.whereareiam.identica.replication.cache.base;

import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.cache.LocalCache;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import org.jetbrains.annotations.NotNull;

/**
 * Fluent builder for cache selection and configuration.
 */
public interface ReplicationCacheBuilder {
	/**
	 * Sets the default TTL applied by caches created from this builder when
	 * callers use {@code put(key, value)} without an explicit TTL.
	 *
	 * @param ttlMs default TTL in milliseconds
	 * @return this builder
	 */
	@NotNull ReplicationCacheBuilder defaultTtl(long ttlMs);

	/**
	 * Creates an in-memory local cache for this namespace.
	 *
	 * @param <T> value type
	 * @return local cache instance
	 */
	@NotNull <T> LocalCache<T> local();

	/**
	 * Creates a replicated cache for this namespace.
	 *
	 * @param type replication type
	 * @param <T> model type
	 * @param <S> snapshot type
	 * @return replicated cache instance
	 */
	@NotNull <T, S> ReplicatedCache<T> replicated(@NotNull ReplicationType<T, S> type);
}
