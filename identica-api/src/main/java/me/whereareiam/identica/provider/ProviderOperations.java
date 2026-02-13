package me.whereareiam.identica.provider;

import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Runtime provider operations used during authentication and resolver resolution.
 */
public interface ProviderOperations {
	/**
	 * Returns eligible providers for the context and flow.
	 *
	 * @param context authentication context
	 * @param flow flow type
	 * @return eligible providers
	 */
	@NotNull List<InternalProvider> eligibleProviders(
			@NotNull ScenarioContext context,
			@NotNull JourneyType flow
	);

	/**
	 * Returns eligible providers for the context, pipeline, and flow.
	 *
	 * @param context authentication context
	 * @param pipelineType pipeline type
	 * @param flow flow type
	 * @return eligible providers
	 */
	default @NotNull List<InternalProvider> eligibleProviders(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow
	) {
		return eligibleProviders(context, flow);
	}

	/**
	 * Checks whether the provider can handle the context for the flow.
	 *
	 * @param context authentication context
	 * @param provider provider instance
	 * @param flow flow type
	 * @return {@code true} if eligible
	 */
	boolean isEligible(
			@NotNull ScenarioContext context,
			@NotNull InternalProvider provider,
			@NotNull JourneyType flow
	);

	/**
	 * Checks whether the provider can handle the context for the pipeline and flow.
	 *
	 * @param context authentication context
	 * @param provider provider instance
	 * @param pipelineType pipeline type
	 * @param flow flow type
	 * @return {@code true} if eligible
	 */
	default boolean isEligible(
			@NotNull ScenarioContext context,
			@NotNull InternalProvider provider,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow
	) {
		return isEligible(context, provider, flow);
	}

	/**
	 * Resolves resolver subject data using loaded providers.
	 *
	 * @param context resolver resolve context
	 * @return resolution or {@code null} when no resolver applies
	 */
	@Nullable ProfileResolution resolveProfile(@NotNull ProfileResolveContext context);
}
