package me.whereareiam.identica.replication.cache.base;

import me.whereareiam.identica.model.replication.ReplicationPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * Generic key/value cache abstraction for replicated or local caches.
 *
 * @param <T> value type
 */
public interface Cache<T> {
	/**
	 * Retrieves a cached entry.
	 *
	 * @param key cache key
	 * @return optional cached value
	 */
	@NotNull CompletableFuture<Optional<T>> get(@Nullable String key);

	/**
	 * Retrieves a cached entry while bypassing stale local replicas when supported.
	 *
	 * <p>The default implementation delegates to {@link #get(String)}.</p>
	 *
	 * @param key cache key
	 * @return optional cached value
	 */
	@NotNull
	default CompletableFuture<Optional<T>> getFresh(@Nullable String key) {
		return get(key);
	}

	/**
	 * Stores a cached entry.
	 *
	 * @param key cache key
	 * @param value cached value
	 * @param ttlMs time-to-live in milliseconds
	 * @return completion journey
	 */
	@NotNull CompletableFuture<Void> put(@Nullable String key, @Nullable T value, long ttlMs);

	/**
	 * Invalidates a cached entry.
	 *
	 * @param key cache key
	 * @return completion journey
	 */
	@NotNull CompletableFuture<Void> invalidate(@Nullable String key);

	/**
	 * Atomically retrieves and invalidates a cached entry when supported by the implementation.
	 *
	 * <p>The default implementation falls back to {@link #get(String)} followed by
	 * {@link #invalidate(String)}.</p>
	 *
	 * @param key cache key
	 * @return optional consumed value
	 */
	@NotNull
	default CompletableFuture<Optional<T>> consume(@Nullable String key) {
		return get(key).thenCompose(value ->
				invalidate(key).thenApply(ignored -> value)
		);
	}

	/**
	 * Lists keys in this cache namespace.
	 *
	 * @param page page number (1-based)
	 * @param pageSize number of entries per page
	 * @return page of keys
	 */
	@NotNull
	default CompletableFuture<ReplicationPage> listKeys(int page, int pageSize) {
		int safePage = Math.max(1, page);
		int safeSize = Math.max(1, pageSize);
		return CompletableFuture.completedFuture(ReplicationPage.empty(safePage, safeSize));
	}
}
