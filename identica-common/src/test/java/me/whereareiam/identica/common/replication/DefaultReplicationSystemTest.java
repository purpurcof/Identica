package me.whereareiam.identica.common.replication;

import me.whereareiam.identica.model.replication.ReplicationEnvelope;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.ReplicationChannel;
import me.whereareiam.identica.replication.cache.LocalCache;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.replication.codec.SnapshotCodec;
import me.whereareiam.identica.replication.codec.SnapshotCodecFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Default Replication System")
class DefaultReplicationSystemTest {
	@DisplayName("Local caches remain usable without replication")
	@Test
	void localCacheIsFunctional() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		DefaultReplicationSystem system = new DefaultReplicationSystem(adapter);

		LocalCache<String> cache = system.cache("local").local();
		cache.put("key", "value", 500).join();

		assertEquals(Optional.of("value"), cache.get("key").join());
	}

	@DisplayName("Replicated caches route writes through the adapter")
	@Test
	void replicatedCacheUsesAdapter() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		DefaultReplicationSystem system = new DefaultReplicationSystem(adapter);
		system.setDefaultCodecFactory(ReplicationTestFixtures.stringCodecFactory());

		ReplicatedCache<String> cache = system.cache("replicated")
				.replicated(ReplicationTestFixtures.stringType());
		cache.put("key", "value", 500).join();

		assertEquals(1, adapter.putCalls);
		assertEquals("replicated", adapter.lastNamespace);
		assertEquals("key", adapter.lastKey);
	}

	@DisplayName("Updating the default codec factory affects existing channels")
	@Test
	void updatedCodecFactoryAffectsExistingChannels() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		DefaultReplicationSystem system = new DefaultReplicationSystem(adapter);

		SnapshotCodecFactory factoryV1 = new SnapshotCodecFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public @NotNull <S> SnapshotCodec<S> codecFor(@NotNull Class<S> snapshotType) {
				SnapshotCodec<String> codec = new SnapshotCodec<>() {
					@Override
					public byte @NotNull [] encode(String snapshot) {
						return ("v1:" + snapshot).getBytes(StandardCharsets.UTF_8);
					}

					@Override
					public String decode(byte[] payload) {
						return new String(payload, StandardCharsets.UTF_8);
					}
				};
				return (SnapshotCodec<S>) codec;
			}
		};

		SnapshotCodecFactory factoryV2 = new SnapshotCodecFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public @NotNull <S> SnapshotCodec<S> codecFor(@NotNull Class<S> snapshotType) {
				SnapshotCodec<String> codec = new SnapshotCodec<>() {
					@Override
					public byte @NotNull [] encode(String snapshot) {
						return ("v2:" + snapshot).getBytes(StandardCharsets.UTF_8);
					}

					@Override
					public String decode(byte[] payload) {
						return new String(payload, StandardCharsets.UTF_8);
					}
				};
				return (SnapshotCodec<S>) codec;
			}
		};

		system.setDefaultCodecFactory(factoryV1);
		ReplicationType<String, String> type = ReplicationType.identity(String.class);
		ReplicationChannel<String> channel = system.channel("channel", type);

		channel.publish("alpha").join();
		ReplicationEnvelope envelopeV1 = ReplicationEnvelope.decode(adapter.lastPublishPayload);
		assertEquals("v1:alpha", new String(envelopeV1.getPayload(), StandardCharsets.UTF_8));

		system.setDefaultCodecFactory(factoryV2);
		channel.publish("beta").join();
		ReplicationEnvelope envelopeV2 = ReplicationEnvelope.decode(adapter.lastPublishPayload);
		assertEquals("v2:beta", new String(envelopeV2.getPayload(), StandardCharsets.UTF_8));
	}
}
