package me.whereareiam.identica.provider.capability.contribution;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Provider-local contribution exposed for a declared capability.
 */
public interface ProviderCapabilityContribution {
	/**
	 * Returns the capability supported by this contribution.
	 *
	 * @return contributed capability
	 */
	@NotNull ProviderCapability capability();
}
