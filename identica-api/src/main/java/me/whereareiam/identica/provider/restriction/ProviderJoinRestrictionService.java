package me.whereareiam.identica.provider.restriction;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionDecision;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Runtime manager and policy service for provider join restrictions.
 */
public interface ProviderJoinRestrictionService {
	/**
	 * Enables runtime restriction for the provider.
	 *
	 * @param providerId provider id
	 * @return resolved status after enabling
	 */
	@NotNull ProviderJoinRestrictionStatus enable(@Nullable String providerId);

	/**
	 * Disables runtime restriction for the provider.
	 *
	 * @param providerId provider id
	 * @return resolved status after disabling
	 */
	@NotNull ProviderJoinRestrictionStatus disable(@Nullable String providerId);

	/**
	 * Returns status for a single provider.
	 *
	 * @param providerId provider id
	 * @return status or empty when provider is unknown
	 */
	@NotNull Optional<ProviderJoinRestrictionStatus> status(@Nullable String providerId);

	/**
	 * Returns statuses for all configured providers.
	 *
	 * @return provider statuses
	 */
	@NotNull List<ProviderJoinRestrictionStatus> statuses();

	/**
	 * Evaluates the current join restriction using only the provider id.
	 *
	 * @param providerId provider id
	 * @return resolved restriction decision
	 */
	@NotNull ProviderJoinRestrictionDecision evaluate(@Nullable String providerId);

	/**
	 * Evaluates the current join restriction for a full provider join attempt.
	 *
	 * @param providerId provider id
	 * @param providerSubject provider subject
	 * @param providerUsername provider username
	 * @param ip player ip
	 * @param origin connection origin
	 * @return resolved restriction decision
	 */
	@NotNull ProviderJoinRestrictionDecision evaluate(
			@Nullable String providerId,
			@Nullable String providerSubject,
			@Nullable String providerUsername,
			@Nullable String ip,
			@Nullable ConnectionIdentity.Origin origin
	);
}
