package me.whereareiam.identica.provider.capability.verification.type;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in verification capability identifier.
 */
public final class VerificationCapability {
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("verification");
}
