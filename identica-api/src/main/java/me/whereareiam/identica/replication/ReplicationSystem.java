package me.whereareiam.identica.replication;

import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.cache.base.ReplicationCacheBuilder;
import me.whereareiam.identica.replication.codec.SnapshotCodecFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for replicated caches and channels.
 */
@SuppressWarnings("unused")
public interface ReplicationSystem {
	/**
	 * Creates a cache builder for the given namespace.
	 *
	 * @param name cache namespace
	 * @return cache builder
	 */
	@NotNull ReplicationCacheBuilder cache(@NotNull String name);

	/**
	 * Creates a typed replication channel.
	 *
	 * @param name channel name
	 * @param type replication type
	 * @param <T> message type
	 * @param <S> snapshot type
	 * @return channel instance
	 */
	@NotNull <T, S> ReplicationChannel<T> channel(
			@NotNull String name,
			@NotNull ReplicationType<T, S> type
	);

	/**
	 * Overrides the default codec factory for all caches and channels.
	 *
	 * @param factory codec factory
	 */
	void setDefaultCodecFactory(@NotNull SnapshotCodecFactory factory);

	/**
	 * Returns the current default codec factory.
	 *
	 * @return codec factory
	 */
	@NotNull SnapshotCodecFactory getDefaultCodecFactory();
}
