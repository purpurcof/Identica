package me.whereareiam.identica.provider.capability.online.type;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in online capability identifier.
 */
public final class OnlineCapability {
	public static final @NotNull ProviderCapability CAPABILITY = ProviderCapability.of("online");
}
