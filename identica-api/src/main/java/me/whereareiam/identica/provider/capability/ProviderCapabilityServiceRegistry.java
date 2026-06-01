package me.whereareiam.identica.provider.capability;

import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared registry for services exposed by installed capability runtimes.
 */
public interface ProviderCapabilityServiceRegistry {
	/**
	 * Registers a typed service for a capability runtime.
	 *
	 * @param capability capability owning the service
	 * @param type service type
	 * @param service service instance
	 * @param <T> service type
	 */
	<T> void register(@NotNull ProviderCapability capability, @NotNull Class<T> type, @NotNull T service);

	/**
	 * Resolves a typed service registered by a capability runtime.
	 *
	 * @param capability capability owning the service
	 * @param type service type
	 * @param <T> service type
	 * @return registered service or {@code null}
	 */
	<T> @Nullable T resolve(@NotNull ProviderCapability capability, @NotNull Class<T> type);
}
