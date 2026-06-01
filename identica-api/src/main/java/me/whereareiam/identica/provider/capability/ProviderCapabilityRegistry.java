package me.whereareiam.identica.provider.capability;

import me.whereareiam.identica.model.provider.capability.ProviderCapabilityInstallation;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registry of installed provider capabilities.
 */
public interface ProviderCapabilityRegistry {
	/**
	 * Finds an installed capability runtime.
	 *
	 * @param capability capability to resolve
	 * @return installed runtime or {@code null}
	 */
	@Nullable ProviderCapabilityInstallation findInstallation(@NotNull ProviderCapability capability);

	/**
	 * Registers an installed capability runtime.
	 *
	 * @param installation installation to register
	 * @return registered installation
	 */
	@NotNull ProviderCapabilityInstallation registerInstallation(@NotNull ProviderCapabilityInstallation installation);

	/**
	 * Removes an installed capability runtime.
	 *
	 * @param capability capability to remove
	 * @return {@code true} when removed
	 */
	boolean unregisterInstallation(@NotNull ProviderCapability capability);

	/**
	 * Returns all installed capability runtimes.
	 *
	 * @return immutable installations list
	 */
	@NotNull List<ProviderCapabilityInstallation> installations();
}
