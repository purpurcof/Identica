package me.whereareiam.identica.model.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.type.provider.ProviderFeature;
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
	 * Declared capability ids resolved from provider-supplied bootstraps.
	 */
	private @NotNull List<String> declaredCapabilityIds = new ArrayList<>();
	/**
	 * Declared feature ids resolved from provider-supplied declarations.
	 */
	private @NotNull List<String> declaredFeatureIds = new ArrayList<>();

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
		for (String entry : declaredCapabilityIds)
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
		if (capabilityId == null || capabilityId.isBlank() || declaredCapabilityIds.isEmpty()) return false;

		String normalized = capabilityId.trim().toLowerCase(Locale.ROOT);
		for (String entry : declaredCapabilityIds) {
			if (entry == null || entry.isBlank()) continue;
			if (normalized.equals(entry.trim().toLowerCase(Locale.ROOT)))
				return true;
		}

		return false;
	}

	/**
	 * Checks whether the provider advertises the feature.
	 *
	 * @param feature feature to check
	 * @return {@code true} when the feature is listed
	 */
	public boolean hasFeature(@Nullable ProviderFeature feature) {
		if (feature == null) return false;
		for (String entry : declaredFeatureIds)
			if (feature.matches(entry)) return true;

		return false;
	}

	/**
	 * Checks whether the provider advertises a feature id.
	 *
	 * @param featureId feature id to check
	 * @return {@code true} when the id is listed
	 */
	public boolean hasFeatureId(@Nullable String featureId) {
		if (featureId == null || featureId.isBlank() || declaredFeatureIds.isEmpty()) return false;

		String normalized = featureId.trim().toLowerCase(Locale.ROOT);
		for (String entry : declaredFeatureIds) {
			if (entry == null || entry.isBlank()) continue;
			if (normalized.equals(entry.trim().toLowerCase(Locale.ROOT))) return true;
		}

		return false;
	}
}
