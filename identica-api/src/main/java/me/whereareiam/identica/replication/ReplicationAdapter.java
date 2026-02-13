package me.whereareiam.identica.replication;

import me.whereareiam.identica.model.replication.ReplicationPage;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Low-level adapter for replication backends (e.g., Redis) that provides
 * key/value storage and pub/sub primitives.
 */
public interface ReplicationAdapter {
	/**
	 * Indicates whether the replication backend is available.
	 *
	 * @return {@code true} when the backend can be used
	 */
	boolean isAvailable();

	/**
	 * Retrieves a value from the replication backend.
	 *
	 * @param namespace logical namespace
	 * @param key lookup key
	 * @return optional payload
	 */
	@NotNull CompletableFuture<Optional<byte[]>> get(@NotNull String namespace, @NotNull String key);

	/**
	 * Atomically retrieves and removes a value from the replication backend.
	 *
	 * @param namespace logical namespace
	 * @param key lookup key
	 * @return optional payload
	 */
	@NotNull CompletableFuture<Optional<byte[]>> consume(@NotNull String namespace, @NotNull String key);

	/**
	 * Stores a value in the replication backend.
	 *
	 * @param namespace logical namespace
	 * @param key lookup key
	 * @param value payload to store
	 * @param ttlMs time-to-live in milliseconds
	 * @return completion journey
	 */
	@NotNull CompletableFuture<Void> put(@NotNull String namespace, @NotNull String key, byte[] value, long ttlMs);

	/**
	 * Invalidates a cached entry.
	 *
	 * @param namespace logical namespace
	 * @param key lookup key
	 * @return completion journey
	 */
	@NotNull CompletableFuture<Void> invalidate(@NotNull String namespace, @NotNull String key);

	/**
	 * Lists keys stored in the replication backend for a namespace.
	 *
	 * @param namespace logical namespace
	 * @param page page number (1-based)
	 * @param pageSize number of entries per page
	 * @return page of keys
	 */
	@NotNull
	default CompletableFuture<ReplicationPage> listKeys(
			@NotNull String namespace,
			int page,
			int pageSize
	) {
		int safePage = Math.max(1, page);
		int safeSize = Math.max(1, pageSize);
		return CompletableFuture.completedFuture(ReplicationPage.empty(safePage, safeSize));
	}

	/**
	 * Publishes a payload to the specified channel.
	 *
	 * @param channel channel name
	 * @param payload payload to publish
	 * @return completion journey
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
