package me.whereareiam.identica.provider.capability.offline.type;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in offline capability identifier.
 */
public final class OfflineCapability {
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("offline");
}
