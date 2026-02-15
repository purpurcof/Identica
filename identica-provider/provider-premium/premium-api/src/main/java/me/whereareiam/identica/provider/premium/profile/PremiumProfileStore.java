package me.whereareiam.identica.provider.premium.profile;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Singleton
public class PremiumProfileStore implements EventListener {
	private static final String KEY_USERNAME_PREFIX = "u:";
	private static final String KEY_USERNAME_IP_PREFIX = "uip:";

	private final @NotNull Provider<PremiumSettings> settingsProvider;
	private final @NotNull ReplicatedCache<PremiumProfileSnapshot> cache;

	@Inject
	public PremiumProfileStore(
			@NotNull Provider<PremiumSettings> settingsProvider,
			@NotNull ReplicationSystem replicationSystem,
			@NotNull EventManager eventManager
	) {
		this.settingsProvider = settingsProvider;
		ReplicationType<PremiumProfileSnapshot, PremiumProfileSnapshot> type =
				ReplicationType.identity(PremiumProfileSnapshot.class);
		this.cache = replicationSystem.cache(resolveNamespace(settingsProvider)).replicated(type);
		eventManager.register(this);
	}

	public void save(@Nullable String username, @Nullable String ip, @NotNull String profileId) {
		List<String> keys = resolveKeys(username, ip);
		if (keys.isEmpty()) return;

		long ttlMs = resolveTtlMs(settingsProvider.get().getProfileSnapshotTtl());
		if (ttlMs <= 0) return;

		PremiumProfileSnapshot snapshot = new PremiumProfileSnapshot(profileId, System.currentTimeMillis());
		for (String key : keys) {
			cache.put(key, snapshot, ttlMs).join();
		}
	}

	public @Nullable PremiumProfileSnapshot find(@Nullable String username, @Nullable String ip) {
		String normalizedUsername = normalize(username);
		if (normalizedUsername == null) return null;

		String normalizedIp = normalize(ip);
		if (normalizedIp != null) {
			String key = KEY_USERNAME_IP_PREFIX + normalizedUsername + "|" + normalizedIp;
			PremiumProfileSnapshot snapshot = cache.get(key).join().orElse(null);
			if (snapshot != null) return snapshot;
		}

		String key = KEY_USERNAME_PREFIX + normalizedUsername;
		return cache.get(key).join().orElse(null);
	}

	public void clear(@Nullable String username, @Nullable String ip) {
		for (String key : resolveKeys(username, ip)) {
			cache.invalidate(key).join();
		}
	}

	@IdenticEvent
	public void onAccountClear(@NotNull AccountClearEvent event) {
		String username = event.getIdentity().getUsername();
		if (username.isBlank()) return;

		clear(username, null);
		clearHostnameScoped(username);
	}

	private @NotNull List<String> resolveKeys(@Nullable String username, @Nullable String ip) {
		List<String> keys = new ArrayList<>(2);

		String normalizedUsername = normalize(username);
		String normalizedIp = normalize(ip);
		if (normalizedUsername == null) return keys;

		if (normalizedIp != null) keys.add(KEY_USERNAME_IP_PREFIX + normalizedUsername + "|" + normalizedIp);
		keys.add(KEY_USERNAME_PREFIX + normalizedUsername);
		return keys;
	}

	private void clearHostnameScoped(@Nullable String username) {
		String normalizedUsername = normalize(username);
		if (normalizedUsername == null) return;

		String prefix = KEY_USERNAME_IP_PREFIX + normalizedUsername + "|";
		List<String> keysToInvalidate = new ArrayList<>();
		int page = 1;
		int pageSize = 200;
		while (true) {
			var result = cache.listKeys(page, pageSize).join();
			if (result == null || result.getEntries().isEmpty())
				break;

			for (String key : result.getEntries()) {
				if (key != null && key.startsWith(prefix))
					keysToInvalidate.add(key);
			}

			boolean hasMoreByTotal = result.getTotal() > (page * pageSize);
			boolean hasMoreByPageSize = result.getEntries().size() >= pageSize;
			if (!hasMoreByTotal && !hasMoreByPageSize)
				break;

			page++;
		}

		for (String key : keysToInvalidate) {
			cache.invalidate(key).join();
		}
	}

	private @Nullable String normalize(@Nullable String value) {
		if (value == null || value.isBlank())
			return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private boolean isUsable(@Nullable Duration duration) {
		return duration != null
				&& !duration.isZero()
				&& !duration.isNegative();
	}

	private long resolveTtlMs(@Nullable Duration duration) {
		if (!isUsable(duration)) return 0;
		return duration.toMillis();
	}

	private static @NotNull String resolveNamespace(Provider<PremiumSettings> settingsProvider) {
		String namespace = settingsProvider.get()
				.getReplication()
				.getCache()
				.getProfileSnapshot();

		if (namespace.isBlank()) {
			throw new IllegalStateException("premium.settings.replication.cache.profileSnapshot is missing");
		}

		return namespace;
	}
}
