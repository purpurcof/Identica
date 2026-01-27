package me.whereareiam.identica.service;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface SynchronizationService {
	/**
	 * Indicates whether the synchronization backend is available.
	 *
	 * @return {@code true} when the backend can be used
	 */
	boolean isAvailable();

	/**
	 * Retrieves a value from the synchronization backend.
	 *
	 * @param namespace logical namespace
	 * @param key lookup key
	 * @return optional payload
	 */
	@NotNull CompletableFuture<Optional<byte[]>> get(@NotNull String namespace, @NotNull String key);

	/**
	 * Stores a value in the synchronization backend.
	 *
	 * @param namespace logical namespace
	 * @param key lookup key
	 * @param value payload to store
	 * @param ttlMs time-to-live in milliseconds
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> put(@NotNull String namespace, @NotNull String key, byte[] value, long ttlMs);

	/**
	 * Invalidates a cached entry.
	 *
	 * @param namespace logical namespace
	 * @param key lookup key
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> invalidate(@NotNull String namespace, @NotNull String key);

	/**
	 * Publishes a payload to the specified channel.
	 *
	 * @param channel channel name
	 * @param payload payload to publish
	 * @return completion stage
	 */
	@NotNull CompletableFuture<Void> publish(@NotNull String channel, byte[] payload);

	/**
	 * Subscribes to a channel and forwards incoming payloads.
	 *
	 * @param channel channel name
	 * @param handler payload handler
	 */
	void subscribe(@NotNull String channel, @NotNull Consumer<byte[]> handler);
}
