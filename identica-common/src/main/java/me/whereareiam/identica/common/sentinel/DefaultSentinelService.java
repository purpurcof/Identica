package me.whereareiam.identica.common.sentinel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.sentinel.SentinelContext;
import me.whereareiam.identica.model.sentinel.SentinelDecision;
import me.whereareiam.identica.model.sentinel.SentinelKey;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.sentinel.SentinelService;
import me.whereareiam.identica.sentinel.SentinelDefinition;
import me.whereareiam.identica.type.sentinel.SentinelMode;
import me.whereareiam.identica.type.sentinel.SentinelScope;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Singleton
public class DefaultSentinelService implements SentinelService {
	private static final String KEY_PREFIX = "rate:";

	private final Registry<SentinelDefinition> registry;
	private final ReplicatedCache<SentinelEntry> cache;

	@Inject
	public DefaultSentinelService(
			Registry<SentinelDefinition> registry,
			ReplicationSystem replicationSystem,
			Provider<Replication> replicationProvider
	) {
		this.registry = registry;
		ReplicationType<SentinelEntry, SentinelEntry> type = ReplicationType.identity(SentinelEntry.class);
		this.cache = replicationSystem.cache(resolveNamespace(replicationProvider)).replicated(type);
	}

	@Override
	public Optional<SentinelDecision> evaluate(SentinelScope scope, SentinelContext ctx) {
		if (scope == null || ctx == null) return Optional.empty();

		SentinelDecision best = null;
		Set<SentinelDefinition> definitions = registry.values();
		for (SentinelDefinition definition : definitions) {
			if (definition == null || !supports(definition, scope)) continue;
			SentinelMode mode = definition.modeFor(scope);
			if (mode == null) continue;

			SentinelPolicy policy = definition.policy(ctx);
			if (!isActive(policy)) continue;

			SentinelKey key = definition.key(ctx);
			if (key == null || key.isBlank()) continue;
			String cacheKey = resolveKey(definition.id(), key);
			if (cacheKey == null) continue;

			SentinelDecision decision = mode == SentinelMode.RECORD
					? recordAttempt(definition, cacheKey, ctx, policy)
					: checkAttempt(definition, cacheKey, ctx, policy);

			if (decision != null && decision.isLimited()) {
				if (best == null) {
					best = decision;
					continue;
				}
				if (decision.isDeny() && !best.isDeny()) {
					best = decision;
					continue;
				}
				if (decision.isDeny() == best.isDeny()
						&& definition.priority() > best.getDefinition().priority()) {
					best = decision;
				}
			}
		}

		return Optional.ofNullable(best);
	}

	@Override
	public SentinelDecision record(String id, SentinelContext ctx) {
		if (id == null || id.isBlank() || ctx == null)
			return SentinelDecision.allowed(null);

		SentinelDefinition definition = findDefinition(id);
		if (definition == null) {
			Logger.warn("Rate limit definition not found: %s", id);
			return SentinelDecision.allowed(null);
		}

		SentinelPolicy policy = definition.policy(ctx);
		if (!isActive(policy)) {
			return SentinelDecision.allowed(definition);
		}

		SentinelKey key = definition.key(ctx);
		if (key == null || key.isBlank()) {
			return SentinelDecision.allowed(definition);
		}

		String cacheKey = resolveKey(definition.id(), key);
		if (cacheKey == null) return SentinelDecision.allowed(definition);

		return recordAttempt(definition, cacheKey, ctx, policy);
	}

	@Override
	public void clear(String id, SentinelContext ctx) {
		if (id == null || id.isBlank() || ctx == null) return;
		SentinelDefinition definition = findDefinition(id);
		if (definition == null) return;

		SentinelKey key = definition.key(ctx);
		if (key == null || key.isBlank()) return;

		String cacheKey = resolveKey(definition.id(), key);
		if (cacheKey == null) return;

		cache.invalidate(cacheKey).join();
	}

	private SentinelDecision checkAttempt(
			SentinelDefinition definition,
			String cacheKey,
			SentinelContext ctx,
			SentinelPolicy policy
	) {
		SentinelEntry entry = read(cacheKey).orElse(null);
		if (entry == null) return null;

		long now = System.currentTimeMillis();
		if (entry.limitedUntil() <= 0) return null;
		if (entry.limitedUntil() <= now) {
			cache.invalidate(cacheKey).join();
			return null;
		}

		long remainingSeconds = (entry.limitedUntil() - now + 999) / 1000;
		return lockoutDecision(definition, ctx, policy, remainingSeconds, false);
	}

	private SentinelDecision recordAttempt(
			SentinelDefinition definition,
			String cacheKey,
			SentinelContext ctx,
			SentinelPolicy policy
	) {
		SentinelEntry entry = read(cacheKey).orElse(null);
		long now = System.currentTimeMillis();
		if (entry != null && entry.limitedUntil() > now) {
			long remainingSeconds = (entry.limitedUntil() - now + 999) / 1000;
			return lockoutDecision(definition, ctx, policy, remainingSeconds, false);
		}

		int attempts = entry != null ? entry.attempts() + 1 : 1;
		long ttlMs = resolveLockTtlMs(policy);
		if (ttlMs <= 0) {
			return SentinelDecision.allowed(definition);
		}

		if (attempts >= policy.getMaxAttempts()) {
			if (isLockoutEnabled(policy)) {
				long limitedUntil = now + ttlMs;
				write(cacheKey, new SentinelEntry(attempts, limitedUntil), ttlMs);
				long remainingSeconds = (limitedUntil - now + 999) / 1000;
				return lockoutDecision(definition, ctx, policy, remainingSeconds, true);
			}
			write(cacheKey, new SentinelEntry(attempts, 0), ttlMs);
			return warningDecision(definition, ctx, policy, attempts);
		}

		write(cacheKey, new SentinelEntry(attempts, 0), ttlMs);
		return warningDecision(definition, ctx, policy, attempts);
	}

	private SentinelDecision lockoutDecision(
			SentinelDefinition definition,
			SentinelContext ctx,
			SentinelPolicy policy,
			long remainingSeconds,
			boolean runCustomAction
	) {
		if (policy == null) return null;
		SentinelPolicy.Lockout lockout = policy.getLockout();
		if (lockout == null || !lockout.isEnabled()) return null;

		if (lockout.getMessageSupplier() != null) {
			String message = lockout.getMessageSupplier().apply(ctx, remainingSeconds);
			boolean deny = message != null && !message.isBlank();
			return SentinelDecision.limited(definition, remainingSeconds, message == null ? "" : message, deny);
		}

		SentinelDecision decision = SentinelDecision.limited(definition, remainingSeconds, "", false);
		if (runCustomAction && lockout.getCustomAction() != null)
			lockout.getCustomAction().accept(ctx, decision);

		return decision;
	}

	private SentinelDecision warningDecision(
			SentinelDefinition definition,
			SentinelContext ctx,
			SentinelPolicy policy,
			int attempts
	) {
		if (policy == null || definition == null) {
			return SentinelDecision.allowed(definition);
		}

		int maxAttempts = policy.getMaxAttempts();
		if (maxAttempts <= 0) return SentinelDecision.allowed(definition);

		SentinelPolicy.Warning warning = policy.getWarning();
		if (warning == null || !warning.isEnabled()) {
			return SentinelDecision.allowed(definition);
		}

		int thresholdPercentage = clampPercentage(warning.getThresholdPercentage());
		if (thresholdPercentage <= 0) return SentinelDecision.allowed(definition);

		int warningThreshold = warningThresholdAttempts(maxAttempts, thresholdPercentage);
		if (attempts < warningThreshold) return SentinelDecision.allowed(definition);

		int remainingAttempts = Math.max(0, maxAttempts - attempts);
		if (warning.getMessageSupplier() != null) {
			String warningMessage = warning.getMessageSupplier().apply(ctx, remainingAttempts);
			if (warningMessage == null || warningMessage.isBlank())
				return SentinelDecision.allowed(definition);

			return SentinelDecision.allowed(definition, remainingAttempts, warningMessage);
		}

		if (warning.getCustomAction() != null) {
			SentinelDecision decision = SentinelDecision.allowed(definition, remainingAttempts, null);
			warning.getCustomAction().accept(ctx, decision);
		}

		return SentinelDecision.allowed(definition);
	}

	private Optional<SentinelEntry> read(String key) {
		return cache.get(key).join();
	}

	private void write(String key, SentinelEntry entry, long ttlMs) {
		if (ttlMs <= 0) return;
		cache.put(key, entry, ttlMs).join();
	}

	private SentinelDefinition findDefinition(String id) {
		Set<SentinelDefinition> definitions = registry.values();
		for (SentinelDefinition definition : definitions) {
			if (definition != null && id.equalsIgnoreCase(definition.id()))
				return definition;
		}
		return null;
	}

	private boolean supports(SentinelDefinition definition, SentinelScope scope) {
		SentinelScope[] scopes = definition.scopes();
		if (scopes == null) return false;
		for (SentinelScope candidate : scopes) {
			if (candidate == scope) return true;
		}

		return false;
	}

	private boolean isActive(SentinelPolicy policy) {
		return policy != null && policy.isActive();
	}

	private String resolveKey(String id, SentinelKey key) {
		String normalizedId = normalize(id);
		String normalizedKey = normalize(key.getKey());
		if (normalizedId == null || normalizedKey == null)
			return null;

		return KEY_PREFIX + normalizedId + "|" + normalizedKey;
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}



	private long resolveLockTtlMs(SentinelPolicy policy) {
		if (policy == null || policy.getLockout() == null || policy.getLockout().getDuration() == null) return 0L;
		long ttlMs = policy.getLockout().getDuration().toMillis();
		return ttlMs > 0 ? ttlMs : 0L;
	}

	private boolean isLockoutEnabled(SentinelPolicy policy) {
		return policy != null && policy.getLockout() != null && policy.getLockout().isEnabled();
	}

	private int warningThresholdAttempts(int maxAttempts, int thresholdPercentage) {
		double percent = thresholdPercentage / 100.0;
		int threshold = (int) Math.floor(maxAttempts * percent) + 1;
		return Math.max(1, threshold);
	}

	private int clampPercentage(int thresholdPercentage) {
		if (thresholdPercentage < 0) return 0;
		return Math.min(thresholdPercentage, 100);
	}

	private static String resolveNamespace(Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null) throw new IllegalStateException("replication is missing");

		String namespace = replication.getCache().getSentinels();
		if (namespace.isBlank()) throw new IllegalStateException("replication.cache.sentinels is missing");

		return namespace;
	}

	public record SentinelEntry(int attempts, long limitedUntil) {
	}
}
