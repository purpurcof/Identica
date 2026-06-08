package me.whereareiam.identica.provider.capability.restriction.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata describing a globally known restriction type.
 */
@Getter
@ToString
@Builder
public final class RestrictionTypeDescriptor {
	private final @NotNull RestrictionType type;
	private final @Nullable String displayName;
	private final @Nullable String description;
}
