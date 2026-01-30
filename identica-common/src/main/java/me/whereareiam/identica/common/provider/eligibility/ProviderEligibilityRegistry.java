package me.whereareiam.identica.common.provider.eligibility;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class ProviderEligibilityRegistry implements Registry<ProviderEligibilityResolver>, Provider<Set<ProviderEligibilityResolver>> {
	private final Set<ProviderEligibilityResolver> resolvers = new CopyOnWriteArraySet<>();

	@Override
	public void register(@NotNull ProviderEligibilityResolver value) {
		resolvers.add(value);
	}

	@Override
	public void unregister(@NotNull ProviderEligibilityResolver value) {
		resolvers.remove(value);
	}

	@Override
	public @NotNull Set<ProviderEligibilityResolver> values() {
		return Collections.unmodifiableSet(resolvers);
	}

	@Override
	public @NotNull Set<ProviderEligibilityResolver> get() {
		return values();
	}
}
