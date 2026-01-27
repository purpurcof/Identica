package me.whereareiam.identica.common.cache.type;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.cache.Cache;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalCache<T> implements Cache<T> {
	private final ConcurrentHashMap<String, Entry<T>> entries = new ConcurrentHashMap<>();

	@Override
	public CompletableFuture<Optional<T>> get(String key) {
		if (key == null) return CompletableFuture.completedFuture(Optional.empty());

		Entry<T> entry = entries.get(key);
		if (entry == null) return CompletableFuture.completedFuture(Optional.empty());
		if (entry.expiresAt > 0 && entry.expiresAt <= System.currentTimeMillis()) {
			entries.remove(key, entry);
			return CompletableFuture.completedFuture(Optional.empty());
		}

		return CompletableFuture.completedFuture(Optional.ofNullable(entry.value));
	}

	@Override
	public CompletableFuture<Void> put(String key, T value, long ttlMs) {
		if (key == null) return CompletableFuture.completedFuture(null);
		if (ttlMs <= 0) {
			entries.remove(key);
			return CompletableFuture.completedFuture(null);
		}

		long expiresAt = System.currentTimeMillis() + ttlMs;
		entries.put(key, new Entry<>(value, expiresAt));

		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> invalidate(String key) {
		if (key != null) entries.remove(key);
		return CompletableFuture.completedFuture(null);
	}

	@RequiredArgsConstructor
	private static final class Entry<T> {
		private final T value;
		private final long expiresAt;
	}
}
