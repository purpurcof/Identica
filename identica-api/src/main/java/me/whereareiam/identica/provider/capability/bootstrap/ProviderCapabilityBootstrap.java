package me.whereareiam.identica.provider.capability.bootstrap;

import com.google.inject.Module;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provider-supplied bootstrap for a supported capability.
 */
public interface ProviderCapabilityBootstrap {
	/**
	 * Returns the capability descriptor exposed by this bootstrap.
	 *
	 * @return capability descriptor
	 */
	@NotNull ProviderCapabilityDescriptor descriptor();

	/**
	 * Initializes capability runtime state after the global injector is created.
	 *
	 * @param context initialization context
	 */
	default void initialize(@NotNull ProviderCapabilityInitializationContext context) {
	}

	/**
	 * Returns modules installed once into a capability-global child injector.
	 *
	 * @param context global installation context
	 * @return modules to install
	 */
	default @NotNull List<Module> globalModules(@NotNull ProviderCapabilityGlobalInstallContext context) {
		return List.of();
	}

	/**
	 * Returns modules installed into a provider child injector.
	 *
	 * @param context provider-local installation context
	 * @return modules to install
	 */
	default @NotNull List<Module> localModules(@NotNull ProviderCapabilityLocalInstallContext context) {
		return List.of();
	}
}
