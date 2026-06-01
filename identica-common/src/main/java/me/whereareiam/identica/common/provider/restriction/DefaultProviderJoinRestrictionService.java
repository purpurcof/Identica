package me.whereareiam.identica.common.provider.restriction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionDecision;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionStatus;
import me.whereareiam.identica.provider.restriction.ProviderJoinRestrictionService;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultProviderJoinRestrictionService implements ProviderJoinRestrictionService {
	private final Provider<Providers> providersProvider;
	private final ProviderJoinRestrictionToggleStore toggleStore;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;

	@Override
	public @NotNull ProviderJoinRestrictionStatus enable(@Nullable String providerId) {
		Providers.ProviderEntry provider = findProvider(providerId);
		if (provider == null) return missing(normalize(providerId));

		Providers.ProviderEntry.Restriction.Join restriction = joinRestriction(provider);
		if (restriction == null || !restriction.isEnabled() || !isConfigured(restriction))
			return unconfigured(provider.getId());

		toggleStore.enable(provider.getId());
		return resolveStatus(provider);
	}

	@Override
	public @NotNull ProviderJoinRestrictionStatus disable(@Nullable String providerId) {
		Providers.ProviderEntry provider = findProvider(providerId);
		if (provider == null) return missing(normalize(providerId));

		toggleStore.disable(provider.getId());
		return resolveStatus(provider);
	}

	@Override
	public @NotNull Optional<ProviderJoinRestrictionStatus> status(@Nullable String providerId) {
		Providers.ProviderEntry provider = findProvider(providerId);
		if (provider == null) return Optional.empty();

		return Optional.of(resolveStatus(provider));
	}

	@Override
	public @NotNull List<ProviderJoinRestrictionStatus> statuses() {
		List<ProviderJoinRestrictionStatus> statuses = new ArrayList<>();
		for (Providers.ProviderEntry provider : providersProvider.get().getProviders()) {
			if (provider == null || provider.getId().isBlank())
				continue;

			statuses.add(resolveStatus(provider));
		}
		return List.copyOf(statuses);
	}

	@Override
	public @NotNull ProviderJoinRestrictionDecision evaluate(@Nullable String providerId) {
		Providers.ProviderEntry provider = findProvider(providerId);
		if (provider == null) return inactiveDecision(Set.of(), Set.of(), false);

		Providers.ProviderEntry.Restriction.Join restriction = joinRestriction(provider);
		Set<ProviderJoinRestrictionCondition> allow = allowSet(restriction);
		if (!isActive(provider, restriction))
			return inactiveDecision(allow, Set.of(), isConfigured(restriction));

		return ProviderJoinRestrictionDecision.builder()
				.allowed(false)
				.configured(isConfigured(restriction))
				.active(true)
				.allow(allow)
				.matchedConditions(Set.of())
				.build();
	}

	@Override
	public @NotNull ProviderJoinRestrictionDecision evaluate(
			@Nullable String providerId,
			@Nullable String providerSubject,
			@Nullable String providerUsername,
			@Nullable String ip,
			@Nullable ConnectionIdentity.Origin origin
	) {
		Providers.ProviderEntry provider = findProvider(providerId);
		if (provider == null)
			return inactiveDecision(Set.of(), Set.of(), false);

		Providers.ProviderEntry.Restriction.Join restriction = joinRestriction(provider);
		Set<ProviderJoinRestrictionCondition> allow = allowSet(restriction);
		if (!isActive(provider, restriction))
			return inactiveDecision(allow, Set.of(), isConfigured(restriction));
		if (allow.isEmpty())
			return deniedDecision(allow, Set.of());

		Set<ProviderJoinRestrictionCondition> matched = new LinkedHashSet<>();
		// TODO restore RECOGNIZED join-restriction matching after allowance
		// evaluation is contributed by capability modules instead of core.
		if (
				allow.contains(ProviderJoinRestrictionCondition.LINKED)
						&& providerSubject != null
						&& !providerSubject.isBlank()
						&& providerLinkPersistenceService.findBySubject(provider.getId(), providerSubject).isPresent()
		)
			matched.add(ProviderJoinRestrictionCondition.LINKED);

		if (!matched.isEmpty())
			return ProviderJoinRestrictionDecision.builder()
					.allowed(true)
					.configured(true)
					.active(true)
					.allow(allow)
					.matchedConditions(Collections.unmodifiableSet(matched))
					.build();

		return deniedDecision(allow, Set.of());
	}

	private @NotNull ProviderJoinRestrictionStatus resolveStatus(@NotNull Providers.ProviderEntry provider) {
		Providers.ProviderEntry.Restriction.Join restriction = joinRestriction(provider);
		Set<ProviderJoinRestrictionCondition> allow = allowSet(restriction);
		return ProviderJoinRestrictionStatus.builder()
				.providerId(provider.getId())
				.active(restriction != null
						&& restriction.isEnabled()
						&& isConfigured(restriction)
						&& toggleStore.isActive(provider.getId()))
				.allow(allow)
				.build();
	}

	private boolean isActive(
			@NotNull Providers.ProviderEntry provider,
			@Nullable Providers.ProviderEntry.Restriction.Join restriction
	) {
		return restriction != null
				&& restriction.isEnabled()
				&& isConfigured(restriction)
				&& toggleStore.isActive(provider.getId());
	}

	private boolean isConfigured(@Nullable Providers.ProviderEntry.Restriction.Join restriction) {
		if (restriction == null || restriction.getAllow() == null)
			return false;

		if (restriction.getAllow().isEmpty())
			return true;

		return !allowSet(restriction).isEmpty();
	}

	private @NotNull Set<ProviderJoinRestrictionCondition> allowSet(
			@Nullable Providers.ProviderEntry.Restriction.Join restriction
	) {
		if (restriction == null || restriction.getAllow() == null)
			return Set.of();

		return restriction.getAllow().stream()
				.filter(Objects::nonNull)
				.filter(condition -> condition != ProviderJoinRestrictionCondition.RECOGNIZED)
				.sorted(Comparator.comparingInt(Enum::ordinal))
				.collect(Collectors.collectingAndThen(
						Collectors.toCollection(LinkedHashSet::new),
						Collections::unmodifiableSet
				));
	}

	private @Nullable Providers.ProviderEntry.Restriction.Join joinRestriction(@NotNull Providers.ProviderEntry provider) {
		Providers.ProviderEntry.Restriction restriction = provider.getRestriction();
		return restriction != null ? restriction.getJoin() : null;
	}

	private @Nullable Providers.ProviderEntry findProvider(@Nullable String providerId) {
		String normalized = normalize(providerId);
		if (normalized == null) return null;

		for (Providers.ProviderEntry provider : providersProvider.get().getProviders()) {
			if (provider == null || provider.getId().isBlank()) continue;
			if (normalized.equals(normalize(provider.getId()))) return provider;
		}

		return null;
	}

	private @Nullable String normalize(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;
		return providerId.trim().toLowerCase(Locale.ROOT);
	}

	private @NotNull ProviderJoinRestrictionStatus missing(@Nullable String providerId) {
		return ProviderJoinRestrictionStatus.builder()
				.providerId(providerId != null ? providerId : "")
				.active(false)
				.allow(Set.of())
				.build();
	}

	private @NotNull ProviderJoinRestrictionStatus unconfigured(@NotNull String providerId) {
		return ProviderJoinRestrictionStatus.builder()
				.providerId(providerId)
				.active(false)
				.allow(Set.of())
				.build();
	}

	private @NotNull ProviderJoinRestrictionDecision inactiveDecision(
			@NotNull Set<ProviderJoinRestrictionCondition> allow,
			@NotNull Set<ProviderJoinRestrictionCondition> matched,
			boolean configured
	) {
		return ProviderJoinRestrictionDecision.builder()
				.allowed(true)
				.configured(configured)
				.active(false)
				.allow(allow)
				.matchedConditions(matched)
				.build();
	}

	private @NotNull ProviderJoinRestrictionDecision deniedDecision(
			@NotNull Set<ProviderJoinRestrictionCondition> allow,
			@NotNull Set<ProviderJoinRestrictionCondition> matched
	) {
		return ProviderJoinRestrictionDecision.builder()
				.allowed(false)
				.configured(true)
				.active(true)
				.allow(allow)
				.matchedConditions(matched)
				.build();
	}
}
