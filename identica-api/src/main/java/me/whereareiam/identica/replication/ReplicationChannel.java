package me.whereareiam.identica.replication;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Typed pub/sub channel for replication events.
 *
 * @param <T> message type
 */
public interface ReplicationChannel<T> {
	/**
	 * Publishes a message to this channel.
	 *
	 * @param message message payload
	 * @return completion journey
	 */
	@NotNull CompletableFuture<Void> publish(@NotNull T message);

	/**
	 * Subscribes to this channel.
	 *
	 * @param handler message handler
	 */
	void subscribe(@NotNull Consumer<T> handler);
}
