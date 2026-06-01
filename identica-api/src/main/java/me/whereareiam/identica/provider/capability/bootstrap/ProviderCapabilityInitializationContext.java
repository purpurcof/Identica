package me.whereareiam.identica.provider.capability.bootstrap;

import com.google.inject.Injector;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context provided when a capability initializes runtime state.
 */
@Getter
@ToString
@Builder
public final class ProviderCapabilityInitializationContext {
	private final @NotNull ProviderCapability capability;
	private final @NotNull Injector rootInjector;
	private final @Nullable Injector globalInjector;
}
