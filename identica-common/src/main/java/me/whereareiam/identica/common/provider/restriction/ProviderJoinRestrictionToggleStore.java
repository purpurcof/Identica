package me.whereareiam.identica.common.provider.restriction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.replication.codec.SnapshotCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Singleton
public class ProviderJoinRestrictionToggleStore {
	private static final String ACTIVE_VALUE = "1";

	private final Provider<Providers> providersProvider;
	private final ReplicatedCache<String> cache;

	@Inject
	public ProviderJoinRestrictionToggleStore(
			@NotNull ReplicationSystem replicationSystem,
			@NotNull Provider<Replication> replicationProvider,
			@NotNull Provider<Providers> providersProvider
	) {
		this.providersProvider = providersProvider;
		ReplicationType<String, String> type = ReplicationType.identity(String.class)
				.withCodec(SnapshotCodec.string());
		this.cache = replicationSystem.cache(resolveNamespace(replicationProvider))
				.replicated(type);
	}

	public boolean isActive(@Nullable String providerId) {
		String key = normalize(providerId);
		if (key == null) return false;

		return cache.get(key).join().filter(ACTIVE_VALUE::equals).isPresent();
	}

	public void enable(@Nullable String providerId) {
		String key = normalize(providerId);
		if (key == null) return;

		cache.put(key, ACTIVE_VALUE, ttlMs()).join();
	}

	public void disable(@Nullable String providerId) {
		String key = normalize(providerId);
		if (key == null) return;

		cache.invalidate(key).join();
	}

	private @Nullable String normalize(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;
		return providerId.trim().toLowerCase(Locale.ROOT);
	}

	private long ttlMs() {
		return providersProvider.get().getBehavior().joinRestrictionToggleTtlMillis();
	}

	private static @NotNull String resolveNamespace(@NotNull Provider<Replication> replicationProvider) {
		String namespace = replicationProvider.get().getCache().getProviderJoinRestrictions();
		if (namespace.isBlank())
			throw new IllegalStateException("replication.cache.providerJoinRestrictions is missing");

		return namespace;
	}
}
