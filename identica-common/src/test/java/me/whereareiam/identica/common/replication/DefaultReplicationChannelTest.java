package me.whereareiam.identica.common.replication;

import me.whereareiam.identica.model.replication.ReplicationEnvelope;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.SnapshotMapper;
import me.whereareiam.identica.replication.codec.SnapshotCodec;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Default Replication Channel")
class DefaultReplicationChannelTest {
	@DisplayName("Publishing wraps snapshots in a replication envelope using the configured codec")
	@Test
	void publishUsesMapperCodecAndEnvelope() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		ReplicationType<String, String> type = ReplicationTestFixtures.stringType();

		DefaultReplicationChannel<String, String> channel = new DefaultReplicationChannel<>(
				"replication-channel",
				adapter,
				type,
				ReplicationTestFixtures.stringCodecFactory()
		);

		channel.publish("hello").join();

		ReplicationEnvelope envelope = ReplicationEnvelope.decode(adapter.lastPublishPayload);
		assertEquals(type.version(), envelope.getVersion());
		assertEquals("hello", SnapshotCodec.string().decode(envelope.getPayload()));
	}

	@DisplayName("Publishing becomes a no-op when the adapter is unavailable")
	@Test
	void publishNoopsWhenAdapterUnavailable() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		adapter.available = false;

		ReplicationType<String, String> type = ReplicationTestFixtures.stringType();
		DefaultReplicationChannel<String, String> channel = new DefaultReplicationChannel<>(
				"replication-channel",
				adapter,
				type,
				ReplicationTestFixtures.stringCodecFactory()
		);

		channel.publish("hello").join();
		assertEquals(0, adapter.publishCalls);
	}

	@DisplayName("Subscriptions ignore invalid payloads and mismatched envelope versions")
	@Test
	void subscribeIgnoresInvalidPayloads() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		ReplicationType<String, String> type = ReplicationTestFixtures.stringType();
		DefaultReplicationChannel<String, String> channel = new DefaultReplicationChannel<>(
				"replication-channel",
				adapter,
				type,
				ReplicationTestFixtures.stringCodecFactory()
		);

		CompletableFuture<String> received = new CompletableFuture<>();
		channel.subscribe(received::complete);

		adapter.emit(new byte[] {1, 2, 3});
		assertThrows(TimeoutException.class, () -> received.get(80, TimeUnit.MILLISECONDS));

		byte[] wrongVersion = ReplicationEnvelope.encode(type.version() + 1, 0L, SnapshotCodec.string().encode("hello"));
		adapter.emit(wrongVersion);
		assertThrows(TimeoutException.class, () -> received.get(80, TimeUnit.MILLISECONDS));
	}

	@DisplayName("Subscriptions ignore snapshot decode failures")
	@Test
	void subscribeIgnoresDecodeErrors() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();

		SnapshotCodec<String> throwingCodec = new SnapshotCodec<>() {
			@Override
			public byte @NotNull [] encode(String snapshot) {
				return SnapshotCodec.string().encode(snapshot);
			}

			@Override
			public String decode(byte[] payload) {
				throw new RuntimeException("boom");
			}
		};

		ReplicationType<String, String> type = ReplicationType.identity(String.class).withCodec(throwingCodec);
		DefaultReplicationChannel<String, String> channel = new DefaultReplicationChannel<>(
				"replication-channel",
				adapter,
				type,
				ReplicationTestFixtures.stringCodecFactory()
		);

		CompletableFuture<String> received = new CompletableFuture<>();
		channel.subscribe(received::complete);

		byte[] envelope = ReplicationEnvelope.encode(type.version(), 0L, throwingCodec.encode("hello"));
		adapter.emit(envelope);

		assertThrows(TimeoutException.class, () -> received.get(80, TimeUnit.MILLISECONDS));
	}

	@DisplayName("Subscriptions wait for asynchronous snapshot mapping to finish")
	@Test
	void subscribeWaitsForMapperCompletion() throws Exception {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		CompletableFuture<String> mapped = new CompletableFuture<>();

		SnapshotMapper<String, String> mapper = new SnapshotMapper<>() {
			@Override
			public String toSnapshot(@NotNull String model) {
				return model;
			}

			@Override
			public @NotNull CompletableFuture<String> fromSnapshot(@NotNull String snapshot) {
				return mapped;
			}
		};

		ReplicationType<String, String> type = ReplicationType.ofSnapshot(String.class, mapper)
				.withCodec(SnapshotCodec.string());

		DefaultReplicationChannel<String, String> channel = new DefaultReplicationChannel<>(
				"replication-channel",
				adapter,
				type,
				ReplicationTestFixtures.stringCodecFactory()
		);

		CompletableFuture<String> received = new CompletableFuture<>();
		channel.subscribe(received::complete);

		byte[] envelope = ReplicationEnvelope.encode(type.version(), 0L, SnapshotCodec.string().encode("hello"));
		adapter.emit(envelope);
		assertFalse(received.isDone());

		mapped.complete("resolved");
		assertEquals("resolved", received.get(1, TimeUnit.SECONDS));
	}

}
