package me.whereareiam.identica.cache;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Cache<T> {
	CompletableFuture<Optional<T>> get(String key);

	CompletableFuture<Void> put(String key, T value, long ttlMs);

	CompletableFuture<Void> invalidate(String key);
}
