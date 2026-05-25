package me.whereareiam.identica.model.provider.restriction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Resolved join restriction decision for a provider join attempt.
 */
@Getter
@ToString
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProviderJoinRestrictionDecision {
	private final boolean allowed;
	private final boolean configured;
	private final boolean active;
	private final @NotNull Set<ProviderJoinRestrictionCondition> allow;
	private final @NotNull Set<ProviderJoinRestrictionCondition> matchedConditions;
}
