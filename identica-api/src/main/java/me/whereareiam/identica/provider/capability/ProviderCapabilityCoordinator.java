package me.whereareiam.identica.provider.capability;

import com.google.inject.Injector;
import com.google.inject.Module;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.contribution.ProviderCapabilityContribution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Validates and installs provider-supplied capabilities during provider load.
 */
public interface ProviderCapabilityCoordinator {
	/**
	 * Resolves provider-supplied capability bootstraps and updates the
	 * descriptor's declared capability ids.
	 *
	 * @param descriptor provider descriptor
	 * @param bootstraps provider bootstraps
	 * @return validated bootstraps in provider-declared order
	 */
	@NotNull List<ProviderCapabilityBootstrap> resolveBootstraps(
			@NotNull ProviderDescriptor descriptor,
			@Nullable List<ProviderCapabilityBootstrap> bootstraps
	);

	/**
	 * Installs shared global capability runtime for the supplied bootstraps.
	 *
	 * @param provider provider being loaded
	 * @param bootstraps validated capability bootstraps
	 */
	void installGlobalCapabilities(
			@NotNull InternalProvider provider,
			@NotNull List<ProviderCapabilityBootstrap> bootstraps
	);

	/**
	 * Resolves provider-local modules for the supplied bootstraps.
	 *
	 * @param provider provider being loaded
	 * @param bootstraps validated capability bootstraps
	 * @return provider-local capability modules
	 */
	@NotNull List<Module> resolveLocalModules(
			@NotNull InternalProvider provider,
			@NotNull List<ProviderCapabilityBootstrap> bootstraps
	);

	/**
	 * Resolves capability contributions from a provider injector.
	 *
	 * @param injector provider injector
	 * @return resolved contributions
	 */
	@NotNull Set<ProviderCapabilityContribution> resolveCapabilityContributions(@NotNull Injector injector);

	/**
	 * Validates resolved contributions against declared capabilities.
	 *
	 * @param descriptor provider descriptor
	 * @param bootstraps validated capability bootstraps
	 * @param contributions resolved contributions
	 */
	void validateCapabilityContributions(
			@NotNull ProviderDescriptor descriptor,
			@NotNull List<ProviderCapabilityBootstrap> bootstraps,
			@NotNull Set<ProviderCapabilityContribution> contributions
	);
}
