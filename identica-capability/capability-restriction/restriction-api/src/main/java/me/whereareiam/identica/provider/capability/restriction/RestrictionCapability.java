package me.whereareiam.identica.provider.capability.restriction;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in restriction capability identifier.
 */
public final class RestrictionCapability {
	/**
	 * Generic restriction capability id.
	 */
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("restriction");
}
