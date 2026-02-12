package me.whereareiam.identica.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Generic key/value cache abstraction used by Identica services.
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
	 * <p>The default implementation delegates to {@link #get(Object)}.</p>
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
	 * <p>The default implementation falls back to {@link #get(Object)} followed by
	 * {@link #invalidate(Object)}.</p>
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
	default CompletableFuture<Page> listKeys(int page, int pageSize) {
		int safePage = Math.max(1, page);
		int safeSize = Math.max(1, pageSize);
		return CompletableFuture.completedFuture(Page.empty(safePage, safeSize));
	}

	/**
	 * Page result for key listings.
	 *
	 * @param entries listed keys
	 * @param page current page
	 * @param pageSize page size
	 * @param total total entries
	 */
	record Page(@NotNull List<String> entries, int page, int pageSize, int total) {
		/**
		 * Creates an empty page.
		 *
		 * @param page page number (1-based)
		 * @param pageSize page size
		 * @return empty page
		 */
		public static @NotNull Page empty(int page, int pageSize) {
			return new Page(List.of(), page, pageSize, 0);
		}
	}
}
