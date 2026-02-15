package me.whereareiam.identica.provider.cracked.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.model.RateLimitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultRateLimitService implements RateLimitService {
	private final ReplicationSystem replicationSystem;
	private final Provider<CrackedSettings> settingsProvider;
	private volatile ReplicatedCache<RateLimitEntry> cache;

	@Override
	public long remainingLimitSeconds(@Nullable String ip) {
		if (ip == null || ip.isBlank())
			return 0;
		RateLimitEntry entry = read(ip).orElse(null);
		if (entry == null)
			return 0;
		long now = System.currentTimeMillis();
		if (entry.limitedUntil() <= now) {
			clear(ip);
			return 0;
		}
		long remainingMs = entry.limitedUntil() - now;
		return (remainingMs + 999) / 1000;
	}

	@Override
	public @NotNull RateLimitResult recordFailure(
			@Nullable String ip,
			int maxAttempts,
			int lockSeconds
	) {
		if (ip == null || ip.isBlank())
			return RateLimitResult.none();
		if (maxAttempts <= 0 || lockSeconds <= 0)
			return RateLimitResult.none();

		long ttlMs = lockSeconds * 1000L;
		long now = System.currentTimeMillis();
		RateLimitEntry entry = read(ip).orElse(new RateLimitEntry(0, 0));

		if (entry.limitedUntil() > now) {
			long remaining = (entry.limitedUntil() - now + 999) / 1000;
			return RateLimitResult.limited(remaining);
		}

		int attempts = entry.attempts() + 1;

		if (attempts >= maxAttempts) {
			write(ip, new RateLimitEntry(attempts, now + ttlMs), ttlMs);
			return RateLimitResult.limited(lockSeconds);
		}

		write(ip, new RateLimitEntry(attempts, 0), ttlMs);
		return RateLimitResult.attempts(attempts);
	}

	@Override
	public void clear(@Nullable String ip) {
		if (ip == null || ip.isBlank()) return;
		cache().invalidate(ipKey(ip)).join();
	}

	private Optional<RateLimitEntry> read(@NotNull String ip) {
		return cache().getFresh(ipKey(ip)).join();
	}

	private void write(@NotNull String ip, @NotNull RateLimitEntry entry, long ttlMs) {
		if (ttlMs <= 0)
			return;
		cache().put(ipKey(ip), entry, ttlMs).join();
	}

	private String ipKey(@NotNull String ip) {
		return ip.trim().toLowerCase();
	}

	private ReplicatedCache<RateLimitEntry> cache() {
		ReplicatedCache<RateLimitEntry> resolved = cache;
		if (resolved != null)
			return resolved;
		synchronized (this) {
			if (cache == null) {
				ReplicationType<RateLimitEntry, RateLimitEntry> type =
						ReplicationType.identity(RateLimitEntry.class);
				cache = replicationSystem.cache(settingsProvider.get().getReplication().getCache().getLockout()).replicated(type);
			}
			return cache;
		}
	}

	public record RateLimitEntry(int attempts, long limitedUntil) {
	}

}
