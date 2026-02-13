package me.whereareiam.identica.replication.cache;

import me.whereareiam.identica.replication.cache.base.Cache;

/**
 * Marker interface for in-memory caches.
 *
 * <p>Local caches never use the replication adapter.</p>
 *
 * @param <T> value type
 */
public interface LocalCache<T> extends Cache<T> {
}
