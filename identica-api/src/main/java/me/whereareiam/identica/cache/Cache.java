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
	 * Stores a cached entry.
	 *
	 * @param key cache key
	 * @param value cached value
	 * @param ttlMs time-to-live in milliseconds
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> put(@Nullable String key, @Nullable T value, long ttlMs);

	/**
	 * Invalidates a cached entry.
	 *
	 * @param key cache key
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> invalidate(@Nullable String key);

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
