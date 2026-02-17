package me.whereareiam.identica.common.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitDecision;
import me.whereareiam.identica.model.ratelimit.RateLimitKey;
import me.whereareiam.identica.model.ratelimit.RateLimitPolicy;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.ratelimit.RateLimitService;
import me.whereareiam.identica.ratelimit.RateLimitDefinition;
import me.whereareiam.identica.type.ratelimit.RateLimitMode;
import me.whereareiam.identica.type.ratelimit.RateLimitScope;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Singleton
public class DefaultRateLimitService implements RateLimitService {
	private static final String KEY_PREFIX = "rate:";

	private final Registry<RateLimitDefinition> registry;
	private final ReplicatedCache<RateLimitEntry> cache;

	@Inject
	public DefaultRateLimitService(
			Registry<RateLimitDefinition> registry,
			ReplicationSystem replicationSystem,
			Provider<Replication> replicationProvider
	) {
		this.registry = registry;
		ReplicationType<RateLimitEntry, RateLimitEntry> type = ReplicationType.identity(RateLimitEntry.class);
		this.cache = replicationSystem.cache(resolveNamespace(replicationProvider)).replicated(type);
	}

	@Override
	public Optional<RateLimitDecision> evaluate(RateLimitScope scope, RateLimitContext ctx) {
		if (scope == null || ctx == null) return Optional.empty();

		RateLimitDecision best = null;
		Set<RateLimitDefinition> definitions = registry.values();
		for (RateLimitDefinition definition : definitions) {
			if (definition == null || !supports(definition, scope)) continue;
			RateLimitMode mode = definition.modeFor(scope);
			if (mode == null) continue;

			RateLimitPolicy policy = definition.policy(ctx);
			if (!isActive(policy)) continue;

			RateLimitKey key = definition.key(ctx);
			if (key == null || key.isBlank()) continue;
			String cacheKey = resolveKey(definition.id(), key);
			if (cacheKey == null) continue;

			RateLimitDecision decision = mode == RateLimitMode.RECORD
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
	public RateLimitDecision record(String id, RateLimitContext ctx) {
		if (id == null || id.isBlank() || ctx == null)
			return RateLimitDecision.allowed(null);

		RateLimitDefinition definition = findDefinition(id);
		if (definition == null) {
			Logger.warn("Rate limit definition not found: %s", id);
			return RateLimitDecision.allowed(null);
		}

		RateLimitPolicy policy = definition.policy(ctx);
		if (!isActive(policy)) {
			return RateLimitDecision.allowed(definition);
		}

		RateLimitKey key = definition.key(ctx);
		if (key == null || key.isBlank()) {
			return RateLimitDecision.allowed(definition);
		}

		String cacheKey = resolveKey(definition.id(), key);
		if (cacheKey == null) return RateLimitDecision.allowed(definition);

		return recordAttempt(definition, cacheKey, ctx, policy);
	}

	@Override
	public void clear(String id, RateLimitContext ctx) {
		if (id == null || id.isBlank() || ctx == null) return;
		RateLimitDefinition definition = findDefinition(id);
		if (definition == null) return;

		RateLimitKey key = definition.key(ctx);
		if (key == null || key.isBlank()) return;

		String cacheKey = resolveKey(definition.id(), key);
		if (cacheKey == null) return;

		cache.invalidate(cacheKey).join();
	}

	private RateLimitDecision checkAttempt(
			RateLimitDefinition definition,
			String cacheKey,
			RateLimitContext ctx,
			RateLimitPolicy policy
	) {
		RateLimitEntry entry = read(cacheKey).orElse(null);
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

	private RateLimitDecision recordAttempt(
			RateLimitDefinition definition,
			String cacheKey,
			RateLimitContext ctx,
			RateLimitPolicy policy
	) {
		RateLimitEntry entry = read(cacheKey).orElse(null);
		long now = System.currentTimeMillis();
		if (entry != null && entry.limitedUntil() > now) {
			long remainingSeconds = (entry.limitedUntil() - now + 999) / 1000;
			return lockoutDecision(definition, ctx, policy, remainingSeconds, false);
		}

		int attempts = entry != null ? entry.attempts() + 1 : 1;
		long ttlMs = resolveLockTtlMs(policy);
		if (ttlMs <= 0) {
			return RateLimitDecision.allowed(definition);
		}

		if (attempts >= policy.getMaxAttempts()) {
			if (isLockoutEnabled(policy)) {
				long limitedUntil = now + ttlMs;
				write(cacheKey, new RateLimitEntry(attempts, limitedUntil), ttlMs);
				long remainingSeconds = (limitedUntil - now + 999) / 1000;
				return lockoutDecision(definition, ctx, policy, remainingSeconds, true);
			}
			write(cacheKey, new RateLimitEntry(attempts, 0), ttlMs);
			return warningDecision(definition, ctx, policy, attempts);
		}

		write(cacheKey, new RateLimitEntry(attempts, 0), ttlMs);
		return warningDecision(definition, ctx, policy, attempts);
	}

	private RateLimitDecision lockoutDecision(
			RateLimitDefinition definition,
			RateLimitContext ctx,
			RateLimitPolicy policy,
			long remainingSeconds,
			boolean runCustomAction
	) {
		if (policy == null) return null;
		RateLimitPolicy.Lockout lockout = policy.getLockout();
		if (lockout == null || !lockout.isEnabled()) return null;

		if (lockout.getMessageSupplier() != null) {
			String message = lockout.getMessageSupplier().apply(ctx, remainingSeconds);
			boolean deny = message != null && !message.isBlank();
			return RateLimitDecision.limited(definition, remainingSeconds, message == null ? "" : message, deny);
		}

		RateLimitDecision decision = RateLimitDecision.limited(definition, remainingSeconds, "", false);
		if (runCustomAction && lockout.getCustomAction() != null)
			lockout.getCustomAction().accept(ctx, decision);

		return decision;
	}

	private RateLimitDecision warningDecision(
			RateLimitDefinition definition,
			RateLimitContext ctx,
			RateLimitPolicy policy,
			int attempts
	) {
		if (policy == null || definition == null) {
			return RateLimitDecision.allowed(definition);
		}

		int maxAttempts = policy.getMaxAttempts();
		if (maxAttempts <= 0) return RateLimitDecision.allowed(definition);

		RateLimitPolicy.Warning warning = policy.getWarning();
		if (warning == null || !warning.isEnabled()) {
			return RateLimitDecision.allowed(definition);
		}

		int thresholdPercentage = clampPercentage(warning.getThresholdPercentage());
		if (thresholdPercentage <= 0) return RateLimitDecision.allowed(definition);

		int warningThreshold = warningThresholdAttempts(maxAttempts, thresholdPercentage);
		if (attempts < warningThreshold) return RateLimitDecision.allowed(definition);

		int remainingAttempts = Math.max(0, maxAttempts - attempts);
		if (warning.getMessageSupplier() != null) {
			String warningMessage = warning.getMessageSupplier().apply(ctx, remainingAttempts);
			if (warningMessage == null || warningMessage.isBlank())
				return RateLimitDecision.allowed(definition);

			return RateLimitDecision.allowed(definition, remainingAttempts, warningMessage);
		}

		if (warning.getCustomAction() != null) {
			RateLimitDecision decision = RateLimitDecision.allowed(definition, remainingAttempts, null);
			warning.getCustomAction().accept(ctx, decision);
		}

		return RateLimitDecision.allowed(definition);
	}

	private Optional<RateLimitEntry> read(String key) {
		return cache.get(key).join();
	}

	private void write(String key, RateLimitEntry entry, long ttlMs) {
		if (ttlMs <= 0) return;
		cache.put(key, entry, ttlMs).join();
	}

	private RateLimitDefinition findDefinition(String id) {
		Set<RateLimitDefinition> definitions = registry.values();
		for (RateLimitDefinition definition : definitions) {
			if (definition != null && id.equalsIgnoreCase(definition.id()))
				return definition;
		}
		return null;
	}

	private boolean supports(RateLimitDefinition definition, RateLimitScope scope) {
		RateLimitScope[] scopes = definition.scopes();
		if (scopes == null) return false;
		for (RateLimitScope candidate : scopes) {
			if (candidate == scope) return true;
		}

		return false;
	}

	private boolean isActive(RateLimitPolicy policy) {
		return policy != null && policy.isActive();
	}

	private String resolveKey(String id, RateLimitKey key) {
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



	private long resolveLockTtlMs(RateLimitPolicy policy) {
		if (policy == null || policy.getLockout() == null || policy.getLockout().getDuration() == null) return 0L;
		long ttlMs = policy.getLockout().getDuration().toMillis();
		return ttlMs > 0 ? ttlMs : 0L;
	}

	private boolean isLockoutEnabled(RateLimitPolicy policy) {
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

		String namespace = replication.getCache().getRateLimits();
		if (namespace.isBlank()) throw new IllegalStateException("replication.cache.rateLimits is missing");

		return namespace;
	}

	public record RateLimitEntry(int attempts, long limitedUntil) {
	}
}
