package me.whereareiam.identica.common.conflict;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.uuid.UniqueIdResolutionSupport;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Providers.ConflictRule;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.common.conflict.resolver.defaults.KickActiveConflictResolver;
import me.whereareiam.identica.common.conflict.resolver.defaults.KickBothConflictResolver;
import me.whereareiam.identica.common.conflict.resolver.defaults.KickJoinerConflictResolver;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.node.ObjectNode;
import me.whereareiam.identica.conflict.resolver.ConflictResolver;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.conflict.ConflictType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Singleton
public class DefaultConflictService implements ConflictService {
	private final Provider<Providers> providersConfig;
	private final Map<String, ConflictResolver> resolvers = new ConcurrentHashMap<>();
	private final Map<String, ConflictType> types = new ConcurrentHashMap<>();

	@Inject
	public DefaultConflictService(
			Provider<Providers> providersConfig,
			KickJoinerConflictResolver kickJoinerResolver,
			KickActiveConflictResolver kickActiveResolver,
			KickBothConflictResolver kickBothResolver
	) {
		this.providersConfig = providersConfig;

		register(kickJoinerResolver);
		register(kickActiveResolver);
		register(kickBothResolver);
	}

	@Override
	public void register(@NotNull ConflictResolver resolver) {
		String id = normalize(resolver.getId());
		if (id == null) return;
		resolvers.put(id, resolver);
	}

	@Override
	public void unregister(@NotNull ConflictResolver resolver) {
		String id = normalize(resolver.getId());
		if (id == null) return;
		resolvers.remove(id);
	}

	@Override
	public @Nullable ConflictResolver getResolver(@NotNull String id) {
		String key = normalize(id);
		if (key == null) return null;
		return resolvers.get(key);
	}

	@Override
	public void register(@NotNull ConflictType type) {
		String key = normalize(type.getKey());
		if (key == null) return;
		types.put(key, type);
		for (ConflictResolver resolver : type.getResolvers())
			register(resolver);
	}

	@Override
	public void unregister(@NotNull ConflictType type) {
		String key = normalize(type.getKey());
		if (key == null) return;
		types.remove(key);
		for (ConflictResolver resolver : type.getResolvers())
			unregister(resolver);
	}

	@Override
	public @Nullable ConflictType getType(@NotNull String key) {
		String normalized = normalize(key);
		if (normalized == null) return null;
		return types.get(normalized);
	}

	@Override
	public @NotNull Set<ConflictType> getTypes() {
		return Set.copyOf(types.values());
	}

	@Override
	public @Nullable ConflictResolution resolve(@NotNull ConflictContext context) {
		ConflictRule rule = resolveRule(context);
		if (rule == null) return null;

		String resolverId = resolveResolverId(rule);
		if (resolverId == null) return null;

		ConflictResolver resolver = getResolver(resolverId);
		if (resolver == null) return null;
		if (!rule.isForce() && !resolver.supports(context.getKey()))
			return null;

		Node params = resolveParams(rule);
		return resolver.resolve(context, params);
	}

	private ConflictRule resolveRule(ConflictContext context) {
		Map<String, Providers.ConflictRules> conflicts = providersConfig.get().getConflicts();
		if (conflicts.isEmpty()) return null;

		Providers.ConflictRules rules = conflicts.get(context.getKey());
		if (rules == null) return null;

		List<ConflictRule> pairs = rules.getPairs();
		if (!pairs.isEmpty()) {
			for (ConflictRule rule : pairs) {
				if (matchesProviders(rule, context))
					return rule;
			}
		}

		return rules.getDefaultRule();
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

	private @Nullable String resolveResolverId(ConflictRule rule) {
		if (rule == null) return null;

		String resolver = rule.getResolver();
		return resolver.isBlank() ? null : resolver;
	}

	private @NotNull Node resolveParams(ConflictRule rule) {
		if (rule == null) return new ObjectNode();

		return rule.getParameters() instanceof ObjectNode objectNode
				? new ObjectNode(objectNode.getValues())
				: new ObjectNode();
	}

	private String normalize(String value) {
		return UniqueIdResolutionSupport.normalize(value);
	}

}
