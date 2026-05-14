package me.whereareiam.identica.common.replication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.replication.cache.DefaultReplicatedCache;
import me.whereareiam.identica.common.replication.cache.InMemoryLocalCache;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.ReplicationAdapter;
import me.whereareiam.identica.replication.ReplicationChannel;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.LocalCache;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.replication.cache.base.ReplicationCacheBuilder;
import me.whereareiam.identica.replication.codec.SnapshotCodec;
import me.whereareiam.identica.replication.codec.SnapshotCodecFactory;
import org.jetbrains.annotations.NotNull;

@Singleton
public final class DefaultReplicationSystem implements ReplicationSystem {
	private final ReplicationAdapter adapter;
	private volatile SnapshotCodecFactory codecFactory;

	private final SnapshotCodecFactory delegatingFactory = new SnapshotCodecFactory() {
		@Override
		public @NotNull <S> SnapshotCodec<S> codecFor(@NotNull Class<S> snapshotType) {
			return codecFactory.codecFor(snapshotType);
		}
	};

	@Inject
	public DefaultReplicationSystem(@NotNull ReplicationAdapter adapter) {
		this.adapter = adapter;
		this.codecFactory = SnapshotCodec::json;
	}

	@Override
	public @NotNull ReplicationCacheBuilder cache(@NotNull String name) {
		return new ReplicationCacheBuilder() {
			private long defaultTtlMs;

			@Override
			public @NotNull ReplicationCacheBuilder defaultTtl(long ttlMs) {
				this.defaultTtlMs = Math.max(0L, ttlMs);
				return this;
			}

			@Override
			public @NotNull <T> LocalCache<T> local() {
				return new InMemoryLocalCache<>(defaultTtlMs);
			}

			@Override
			public @NotNull <T, S> ReplicatedCache<T> replicated(
					@NotNull ReplicationType<T, S> type
			) {
				return new DefaultReplicatedCache<>(
						name,
						new InMemoryLocalCache<>(defaultTtlMs),
						adapter,
						type,
						delegatingFactory,
						defaultTtlMs
				);
			}
		};
	}

	@Override
	public @NotNull <T, S> ReplicationChannel<T> channel(
			@NotNull String name,
			@NotNull ReplicationType<T, S> type
	) {
		return new DefaultReplicationChannel<>(name, adapter, type, delegatingFactory);
	}

	@Override
	public void setDefaultCodecFactory(@NotNull SnapshotCodecFactory factory) {
		this.codecFactory = factory;
	}

	@Override
	public @NotNull SnapshotCodecFactory getDefaultCodecFactory() {
		return codecFactory;
	}
}
