package me.whereareiam.identica.model.provider.capability;

import com.google.inject.Injector;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Installed runtime state for a capability bootstrap.
 */
@Getter
@ToString
@Builder
public final class ProviderCapabilityInstallation {
	private final @NotNull ProviderCapabilityBootstrap bootstrap;
	private final @Nullable Injector globalInjector;
}
