package me.whereareiam.identica.model.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.type.provider.ProviderCapability;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Descriptor for a provider module.
 */
@Getter
@Setter
@ToString
public class ProviderDescriptor {
	private String id;
	private String name;
	private String version;

	private String main;
	private List<String> authors;
	private List<String> supportedPlatforms;

	/**
	 * Provider capability ids.
	 */
	private List<ProviderCapability> capabilities = new ArrayList<>();

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
		if (capabilities == null || capabilities.isEmpty()) return false;
		for (ProviderCapability entry : capabilities)
			if (entry == capability) return true;

		return false;
	}

	/**
	 * Checks whether the provider advertises a capability id.
	 *
	 * @param capabilityId capability id to check
	 * @return {@code true} when the id is listed
	 */
	public boolean hasCapabilityId(@Nullable String capabilityId) {
		return hasCapability(ProviderCapability.fromId(capabilityId));
	}
}
