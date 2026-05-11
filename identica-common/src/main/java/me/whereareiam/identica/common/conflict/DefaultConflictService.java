package me.whereareiam.identica.common.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.util.UniqueIdResolutionSupport;
import me.whereareiam.identica.conflict.ConflictGuard;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Providers.ConflictRule;
import me.whereareiam.identica.model.config.Providers.ResolverEntry;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.common.conflict.resolver.defaults.KickActiveConflictResolver;
import me.whereareiam.identica.common.conflict.resolver.defaults.KickBothConflictResolver;
import me.whereareiam.identica.common.conflict.resolver.defaults.KickJoinerConflictResolver;
import me.whereareiam.identica.conflict.resolver.ConflictResolver;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.conflict.ConflictType;
import me.whereareiam.identica.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Singleton
public class DefaultConflictService implements ConflictService {
	private final Provider<Providers> providersConfig;
	private final Set<ConflictGuard> globalGuards;
	private final Map<String, ConflictResolver> resolvers = new ConcurrentHashMap<>();
	private final Map<String, ConflictType> types = new ConcurrentHashMap<>();

	@Inject
	public DefaultConflictService(
			Provider<Providers> providersConfig,
			Set<ConflictGuard> globalGuards,
			KickJoinerConflictResolver kickJoinerResolver,
			KickActiveConflictResolver kickActiveResolver,
			KickBothConflictResolver kickBothResolver
	) {
		this.providersConfig = providersConfig;
		this.globalGuards = globalGuards;

		register(kickJoinerResolver);
		register(kickActiveResolver);
		register(kickBothResolver);
	}

	@Override
	public void register(@NotNull ConflictResolver resolver) {
		String id = UniqueIdResolutionSupport.normalize(resolver.getId());
		if (id == null) return;
		resolvers.put(id, resolver);
	}

	@Override
	public void unregister(@NotNull ConflictResolver resolver) {
		String id = UniqueIdResolutionSupport.normalize(resolver.getId());
		if (id == null) return;
		resolvers.remove(id);
	}

	@Override
	public @Nullable ConflictResolver getResolver(@NotNull String id) {
		String key = UniqueIdResolutionSupport.normalize(id);
		if (key == null) return null;
		return resolvers.get(key);
	}

	@Override
	public void register(@NotNull ConflictType type) {
		String key = UniqueIdResolutionSupport.normalize(type.getKey());
		if (key == null) return;
		types.put(key, type);
		for (ConflictResolver resolver : type.getResolvers())
			register(resolver);
	}

	@Override
	public void unregister(@NotNull ConflictType type) {
		String key = UniqueIdResolutionSupport.normalize(type.getKey());
		if (key == null) return;
		types.remove(key);
		for (ConflictResolver resolver : type.getResolvers())
			unregister(resolver);
	}

	@Override
	public @Nullable ConflictType getType(@NotNull String key) {
		String normalized = UniqueIdResolutionSupport.normalize(key);
		if (normalized == null) return null;
		return types.get(normalized);
	}

	@Override
	public @NotNull Set<ConflictType> getTypes() {
		return Set.copyOf(types.values());
	}

	@Override
	public @Nullable ConflictResolution resolve(@NotNull ConflictContext context) {
		Providers.ConflictRules rules = resolveRules(context);
		if (rules == null) return null;

		ConflictType type = getType(context.getKey());
		ConflictResolution guardResolution = applyGuards(context, type);
		if (guardResolution != null) return guardResolution;

		ConflictRule pairRule = resolvePairRule(rules, context);
		if (pairRule != null) {
			ConflictResolution resolution = resolveRule(context, pairRule);
			if (resolution.getAction() != ConflictResolution.Action.PASS) return resolution;
		}

		ConflictRule defaultRule = rules.getDefaultRule();
		ConflictResolution resolution = resolveRule(context, defaultRule);
		if (resolution.getAction() == ConflictResolution.Action.PASS) {
			Logger.warn("Conflict resolution passed for key: %s", context.getKey());
			return ConflictResolution.allow();
		}

		return resolution;
	}

	private @Nullable Providers.ConflictRules resolveRules(ConflictContext context) {
		Map<String, Providers.ConflictRules> conflicts = providersConfig.get().getConflicts();
		if (conflicts.isEmpty()) return null;

		return conflicts.get(context.getKey());
	}

	private @Nullable ConflictRule resolvePairRule(
			@NotNull Providers.ConflictRules rules,
			@NotNull ConflictContext context
	) {
		List<ConflictRule> pairs = rules.getPairs();
		if (pairs.isEmpty()) return null;

		for (ConflictRule rule : pairs) {
			if (matchesProviders(rule, context))
				return rule;
		}
		return null;
	}

	private boolean matchesProviders(ConflictRule rule, ConflictContext context) {
		if (rule == null || rule.getProviders().isEmpty())
			return false;

		AccountProviderLink incoming = context.getIncomingLink();
		AccountProviderLink existing = context.getExistingLink();
		if (incoming == null || existing == null) return false;

		String incomingProvider = incoming.getProviderId();
		String existingProvider = existing.getProviderId();

		boolean hasIncoming = false;
		boolean hasExisting = false;
		for (String provider : rule.getProviders()) {
			if (provider == null || provider.isBlank()) continue;
			if (provider.equalsIgnoreCase(incomingProvider)) hasIncoming = true;
			if (provider.equalsIgnoreCase(existingProvider)) hasExisting = true;
		}

		return hasIncoming && hasExisting;
	}

	private @Nullable ConflictResolution applyGuards(
			@NotNull ConflictContext context,
			@Nullable ConflictType type
	) {
		for (ConflictGuard guard : globalGuards) {
			if (guard == null) continue;
			ConflictResolution resolution = guard.guard(context);
			if (resolution != null) return resolution;
		}

		if (type == null) return null;
		for (ConflictGuard guard : type.getGuards()) {
			if (guard == null) continue;
			ConflictResolution resolution = guard.guard(context);
			if (resolution != null) return resolution;
		}

		return null;
	}

	private @NotNull ConflictResolution resolveRule(
			@NotNull ConflictContext context,
			@NotNull ConflictRule rule
	) {
		List<ResolverEntry> entries = rule.getResolvers();
		if (entries.isEmpty()) return ConflictResolution.pass();

		for (ResolverEntry entry : entries) {
			if (entry == null) continue;
			String resolverId = resolveResolverId(entry);
			if (resolverId == null) continue;

			ConflictResolver resolver = getResolver(resolverId);
			if (resolver == null) {
				Logger.warn("Conflict resolver not found: %s", resolverId);
				continue;
			}

			if (!rule.isForce() && !resolver.supports(context.getKey()))
				continue;

			JsonNode params = resolveParams(entry);
			ConflictResolution resolution = resolver.resolve(context, params);
			if (resolution.getAction() == ConflictResolution.Action.PASS)
				continue;

			return resolution;
		}

		return ConflictResolution.pass();
	}

	private @Nullable String resolveResolverId(ResolverEntry entry) {
		if (entry == null) return null;

		String resolver = entry.getId();
		return resolver.isBlank() ? null : resolver;
	}

	private @NotNull JsonNode resolveParams(ResolverEntry entry) {
		if (entry == null) return JsonNodeFactory.instance.objectNode();

		return entry.getParameters() instanceof ObjectNode objectNode
				? objectNode.deepCopy()
				: JsonNodeFactory.instance.objectNode();
	}
}
