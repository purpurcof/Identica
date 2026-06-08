package me.whereareiam.identica.provider.capability.bootstrap;

import com.google.inject.Injector;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Context provided when a capability installs global runtime services.
 */
@Getter
@ToString
@Builder
public final class ProviderCapabilityGlobalInstallContext {
	private final @NotNull ProviderCapability capability;
	private final @NotNull Injector rootInjector;
	private final @NotNull Path capabilitiesPath;
}
