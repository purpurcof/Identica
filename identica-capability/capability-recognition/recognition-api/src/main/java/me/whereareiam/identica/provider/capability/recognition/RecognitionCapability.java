package me.whereareiam.identica.provider.capability.recognition;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in reconnect recognition capability identifier.
 */
public final class RecognitionCapability {
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("recognition");
}
