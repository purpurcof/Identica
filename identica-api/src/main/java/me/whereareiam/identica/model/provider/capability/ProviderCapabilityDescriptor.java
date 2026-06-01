package me.whereareiam.identica.model.provider.capability;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityScope;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Runtime descriptor exposed by a capability bootstrap.
 */
@Getter
@ToString
@Builder
public final class ProviderCapabilityDescriptor {
	private final @NotNull ProviderCapability capability;
	@Builder.Default
	private final @NotNull Set<ProviderCapabilityScope> scopes = Set.of();
	private final boolean requiresContribution;
}
