package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.common.provider.restriction.ProviderJoinRestrictionToggleStore;
import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.replication.ReplicationPage;
import me.whereareiam.identica.replication.ReplicationAdapter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Provider Join-Restriction Toggle Store")
class ProviderJoinRestrictionToggleStoreTest {
	@DisplayName("Enable and disable are normalized by provider id")
	@Test
	void enableAndDisableNormalizeProviderId() {
		ProviderJoinRestrictionToggleStore store = new ProviderJoinRestrictionToggleStore(
				new DefaultReplicationSystem(new InMemoryReplicationAdapter()),
				this::replication,
				this::settings
		);

		store.enable("Premium");
		assertTrue(store.isActive("premium"));
		assertTrue(store.isActive(" PREMIUM "));

		store.disable("premium");
		assertFalse(store.isActive("Premium"));
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		cache.setProviderJoinRestrictions("provider-join-restrictions");
		replication.setCache(cache);
		return replication;
	}

	private Providers settings() {
		Providers settings = new Providers();
		Providers.Behavior behavior = new Providers.Behavior();
		behavior.setJoinRestrictionToggleTtl(java.time.Duration.ofDays(365));
		settings.setBehavior(behavior);
		return settings;
	}

	private static final class InMemoryReplicationAdapter implements ReplicationAdapter {
		private final Map<String, byte[]> entries = new ConcurrentHashMap<>();

		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public @NotNull CompletableFuture<Optional<byte[]>> get(@NotNull String namespace, @NotNull String key) {
			return CompletableFuture.completedFuture(Optional.ofNullable(entries.get(namespace + ":" + key)));
		}

		@Override
		public @NotNull CompletableFuture<Optional<byte[]>> consume(@NotNull String namespace, @NotNull String key) {
			return CompletableFuture.completedFuture(Optional.ofNullable(entries.remove(namespace + ":" + key)));
		}

		@Override
		public @NotNull CompletableFuture<Void> put(@NotNull String namespace, @NotNull String key, byte[] value, long ttlMs) {
			entries.put(namespace + ":" + key, value);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NotNull CompletableFuture<Void> invalidate(@NotNull String namespace, @NotNull String key) {
			entries.remove(namespace + ":" + key);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NotNull CompletableFuture<ReplicationPage> listKeys(@NotNull String namespace, int page, int pageSize) {
			return CompletableFuture.completedFuture(ReplicationPage.empty(page, pageSize));
		}

		@Override
		public @NotNull CompletableFuture<Void> publish(@NotNull String channel, byte[] payload) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void subscribe(@NotNull String channel, @NotNull java.util.function.Consumer<byte[]> handler) {
		}
	}
}
