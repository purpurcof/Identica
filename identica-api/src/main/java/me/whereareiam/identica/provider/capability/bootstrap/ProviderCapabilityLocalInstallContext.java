package me.whereareiam.identica.provider.capability.bootstrap;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.provider.capability.ProviderCapabilityServiceRegistry;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Context provided when a capability installs provider-local runtime modules.
 */
@Getter
@ToString
@Builder
public final class ProviderCapabilityLocalInstallContext {
	private final @NotNull ProviderCapability capability;
	private final @NotNull String providerId;
	private final @NotNull Path workingPath;
	private final @NotNull ProviderCapabilityServiceRegistry serviceRegistry;
}
