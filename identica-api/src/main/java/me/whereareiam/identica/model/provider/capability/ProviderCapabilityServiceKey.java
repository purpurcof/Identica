package me.whereareiam.identica.model.provider.capability;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

/**
 * Typed lookup key for services exposed by a capability runtime.
 *
 * @param <T> service type
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
public final class ProviderCapabilityServiceKey<T> {
	private final @NotNull ProviderCapability capability;
	private final @NotNull Class<T> type;
}
