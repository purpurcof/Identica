package me.whereareiam.identica.synchronization;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface SynchronizationService {
	boolean isAvailable();

	CompletableFuture<Optional<byte[]>> get(String namespace, String key);

	CompletableFuture<Void> put(String namespace, String key, byte[] value, long ttlMs);

	CompletableFuture<Void> invalidate(String namespace, String key);

	CompletableFuture<Void> publish(String channel, byte[] payload);

	void subscribe(String channel, Consumer<byte[]> handler);
}
