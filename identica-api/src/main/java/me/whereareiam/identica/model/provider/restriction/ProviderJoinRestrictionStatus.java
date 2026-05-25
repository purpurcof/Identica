package me.whereareiam.identica.model.provider.restriction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Resolved provider join restriction status.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
public class ProviderJoinRestrictionStatus {
	private final @NotNull String providerId;
	private final boolean active;

	private final @NotNull Set<ProviderJoinRestrictionCondition> allow;
}
