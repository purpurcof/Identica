package me.whereareiam.identica.feature.verification;

import me.whereareiam.identica.type.provider.ProviderFeature;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in verification feature identifier.
 */
public final class VerificationFeature {
	public static final @NotNull ProviderFeature FEATURE = ProviderFeature.of("verification");
}
