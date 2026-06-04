package me.whereareiam.identica.provider.capability.restriction.join;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.capability.restriction.RestrictionTypeResolver;
import me.whereareiam.identica.provider.capability.restriction.join.config.provider.JoinRestrictionCapabilities;
import me.whereareiam.identica.provider.capability.restriction.join.config.provider.JoinRestrictionProvidersProvider;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionTypeState;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionSignal;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class JoinRestrictionTypeResolver implements RestrictionTypeResolver {
	private final ProviderManager providerManager;
	private final JoinRestrictionProvidersProvider providersProvider;

	@Override
	public @NotNull RestrictionType type() {
		return JoinRestrictionType.TYPE;
	}

	@Override
	public @NotNull Optional<RestrictionTypeState> resolve(@Nullable String providerId) {
		InternalProvider provider = findProvider(providerId);
		if (provider == null || provider.getDescriptor() == null) return Optional.empty();

		String resolvedProviderId = provider.getDescriptor().getId();
		Providers.ProviderEntry entry = findEntry(resolvedProviderId);
		JoinRestrictionCapabilities.Restriction.Join join = join(entry);
		if (join == null)
			return Optional.of(new RestrictionTypeState(resolvedProviderId, false, false, Set.of()));

		Set<RestrictionSignal> allow = allow(join);
		return Optional.of(new RestrictionTypeState(resolvedProviderId, join.isEnabled(), true, allow));
	}

	@Override
	public @NotNull List<RestrictionTypeState> resolveAll() {
		List<RestrictionTypeState> states = new ArrayList<>();
		for (InternalProvider provider : providerManager.getProviders()) {
			if (provider == null || provider.getDescriptor() == null) continue;
			if (!provider.getDescriptor().hasCapability(JoinRestrictionCapability.CAPABILITY)) continue;

			resolve(provider.getDescriptor().getId()).ifPresent(states::add);
		}

		return List.copyOf(states);
	}

	private @Nullable InternalProvider findProvider(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;
		for (InternalProvider provider : providerManager.getProviders()) {
			if (provider == null || provider.getDescriptor() == null) continue;
			if (provider.getDescriptor().getId().equalsIgnoreCase(providerId))
				return provider;
		}
		return null;
	}

	private @Nullable Providers.ProviderEntry findEntry(@NotNull String providerId) {
		for (Providers.ProviderEntry entry : providersProvider.get().getProviders()) {
			if (entry == null || entry.getId().isBlank()) continue;
			if (entry.getId().equalsIgnoreCase(providerId))
				return entry;
		}

		return null;
	}

	private @Nullable JoinRestrictionCapabilities.Restriction.Join join(@Nullable Providers.ProviderEntry entry) {
		if (entry == null) return null;
		if (!(entry.getCapabilities() instanceof JoinRestrictionCapabilities capabilities)) return null;
		if (capabilities.getRestriction() == null) return null;

		return capabilities.getRestriction().getJoin();
	}

	private @NotNull Set<RestrictionSignal> allow(@NotNull JoinRestrictionCapabilities.Restriction.Join join) {
		LinkedHashSet<RestrictionSignal> resolved = new LinkedHashSet<>();
		for (String signal : join.getAllow()) {
			if (signal == null || signal.isBlank()) continue;
			resolved.add(RestrictionSignal.of(signal.trim().toLowerCase(Locale.ROOT)));
		}

		return Set.copyOf(resolved);
	}
}
