package me.whereareiam.identica.provider.capability.restriction.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionSignal;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata describing a globally known restriction signal.
 */
@Getter
@ToString
@Builder
public final class RestrictionSignalDescriptor {
	private final @NotNull RestrictionType restrictionType;
	private final @NotNull RestrictionSignal signal;
	private final @Nullable String displayName;
	private final @Nullable String description;
}
