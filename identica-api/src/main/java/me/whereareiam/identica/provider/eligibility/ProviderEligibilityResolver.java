package me.whereareiam.identica.provider.eligibility;

import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves whether a provider can handle a context for a journeyMode.
 */
public interface ProviderEligibilityResolver {
	/**
	 * Checks whether the provider can handle the context.
	 *
	 * @param context authentication context
	 * @param provider provider instance
	 * @param journeyMode current journeyMode
	 * @return {@code true} if the provider can handle the context
	 */
	boolean isEligible(
			@NotNull ScenarioContext context,
			@NotNull InternalProvider provider,
			@NotNull JourneyMode journeyMode
	);
}
