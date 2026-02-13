package me.whereareiam.identica.replication.cache;

import me.whereareiam.identica.replication.cache.base.Cache;

/**
 * Marker interface for replicated caches.
 *
 * <p>Replicated caches fall back to local behavior when the adapter is unavailable.</p>
 *
 * @param <T> value type
 */
public interface ReplicatedCache<T> extends Cache<T> {
}
