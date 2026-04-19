package me.whereareiam.identica.common.adapter;

import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.adapter.ProfileRewriteAdapter;
import me.whereareiam.identica.handshake.HandshakeDecisionAdapter;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.prepare.PrepareRequest;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrepareRequestConnectionKeyTest {
	@Test
	void profileRewritePassesConnectionKeyIntoPrepareRequest() {
		TestConnectionCoordinator connectionCoordinator = new TestConnectionCoordinator();
		TestProfileRewriteAdapter adapter = new TestProfileRewriteAdapter(connectionCoordinator);
		ConnectionIdentity identity = identity("PlayerOne");

		adapter.run(new ProfileRewriteAdapter.ProfileRewriteRequest(
						identity,
						UUID.randomUUID(),
						identity.getUsername()
				))
				.toCompletableFuture()
				.join();

		PrepareRequest captured = connectionCoordinator.lastPrepareRequest;
		assertEquals(identity.connectionKey(), captured.getConnectionKey());
	}

	@Test
	void handshakePassesConnectionKeyIntoPrepareRequest() {
		TestConnectionCoordinator connectionCoordinator = new TestConnectionCoordinator();
		HandshakeStore handshakeStore = mock(HandshakeStore.class);
		when(handshakeStore.consumeInstruction(any(), any())).thenReturn(Optional.empty());

		TestHandshakeDecisionAdapter adapter = new TestHandshakeDecisionAdapter(
				connectionCoordinator,
				handshakeStore
		);
		ConnectionIdentity identity = identity("PlayerTwo");

		adapter.run(new HandshakeDecisionAdapter.HandshakeAdapterRequest(
						identity,
						instruction -> {
						}
				))
				.toCompletableFuture()
				.join();

		PrepareRequest captured = connectionCoordinator.lastPrepareRequest;
		assertEquals(identity.connectionKey(), captured.getConnectionKey());
	}

	private @NotNull ConnectionIdentity identity(@NotNull String username) {
		ConnectionIdentity identity = new ConnectionIdentity(username, "127.0.0.1");
		identity.setOrigin(new ConnectionIdentity.Origin("play.example.com", 25565));
		return identity;
	}

	private static final class TestConnectionCoordinator implements ConnectionCoordinator {
		private PrepareRequest lastPrepareRequest;

		@Override
		public @NotNull CompletableFuture<PrepareDecision> prepare(PrepareRequest request) {
			lastPrepareRequest = request;
			return CompletableFuture.completedFuture(PrepareDecision.allow());
		}

		@Override
		public @NotNull CompletableFuture<me.whereareiam.identica.model.auth.ConnectionDecision> process(
				me.whereareiam.identica.model.auth.request.ConnectionRequest request
		) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<me.whereareiam.identica.model.auth.ConnectionDecision> resume(
				me.whereareiam.identica.model.auth.request.ResumeRequest request
		) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<me.whereareiam.identica.model.auth.ConnectionDecision> advance(
				me.whereareiam.identica.model.auth.request.AdvanceRequest request
		) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasPending(@NotNull UUID connectionUniqueId) {
			return false;
		}
	}

	private static final class TestProfileRewriteAdapter extends ProfileRewriteAdapter {
		private TestProfileRewriteAdapter(@NotNull ConnectionCoordinator connectionCoordinator) {
			super(connectionCoordinator, new NoopPrepareStateStore());
		}

		private CompletableFuture<Void> run(@NotNull ProfileRewriteAdapter.ProfileRewriteRequest request) {
			return adapt(request, rewrite -> {
			}).toCompletableFuture();
		}
	}

	private static final class TestHandshakeDecisionAdapter extends HandshakeDecisionAdapter {
		private TestHandshakeDecisionAdapter(
				@NotNull ConnectionCoordinator connectionCoordinator,
				@NotNull HandshakeStore handshakeStore
		) {
			super(connectionCoordinator, new NoopPrepareStateStore(), handshakeStore, Messages::new);
		}

		private CompletableFuture<Void> run(@NotNull HandshakeDecisionAdapter.HandshakeAdapterRequest request) {
			return adapt(request, message -> {
			}).toCompletableFuture();
		}
	}

	private static final class NoopPrepareStateStore implements PrepareStateStore {
		@Override
		public void put(@NotNull String connectionKey, @NotNull PrepareDecision decision) {
		}

		@Override
		public void put(@NotNull UUID uniqueId, String connectionKey, @NotNull PrepareDecision decision) {
		}

		@Override
		public @NotNull Optional<PrepareDecision> peek(@NotNull String connectionKey) {
			return Optional.empty();
		}

		@Override
		public @NotNull Optional<PrepareDecision> peek(@NotNull UUID uniqueId) {
			return Optional.empty();
		}

		@Override
		public boolean clear(@NotNull UUID uniqueId) {
			return false;
		}
	}
}
