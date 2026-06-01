package me.whereareiam.identica.model.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Descriptor for a provider module.
 */
@Getter
@Setter
@ToString
@SuppressWarnings("unused")
public class ProviderDescriptor {
	private @NotNull String id;
	private @NotNull String name;
	private @NotNull String version;

	private @NotNull String main;
	private @Nullable List<String> authors;
	private @NotNull List<String> supportedPlatforms;

	/**
	 * Provider capability ids.
	 */
	private @NotNull List<String> capabilities = new ArrayList<>();

	private int priority = 0;

	private ProviderLibraries libraries;

	/**
	 * Checks whether the provider advertises the capability.
	 *
	 * @param capability capability to check
	 * @return {@code true} when the capability is listed
	 */
	public boolean hasCapability(@Nullable ProviderCapability capability) {
		if (capability == null) return false;
		for (String entry : capabilities)
			if (capability.matches(entry))
				return true;

		return false;
	}

	/**
	 * Checks whether the provider advertises a capability id.
	 *
	 * @param capabilityId capability id to check
	 * @return {@code true} when the id is listed
	 */
	public boolean hasCapabilityId(@Nullable String capabilityId) {
		if (capabilityId == null || capabilityId.isBlank() || capabilities.isEmpty()) return false;

		String normalized = capabilityId.trim().toLowerCase(Locale.ROOT);
		for (String entry : capabilities) {
			if (entry == null || entry.isBlank()) continue;
			if (normalized.equals(entry.trim().toLowerCase(Locale.ROOT)))
				return true;
		}

		return false;
	}
}
