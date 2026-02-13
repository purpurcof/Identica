package me.whereareiam.identica.common.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.provider.ProviderEligibilityEvent;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.pipeline.journey.registry.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.pipeline.journey.registry.JourneyRegistry;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.pipeline.journey.registry.RegistrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.MigrationJourneyRegistry;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.type.provider.ProviderState;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultProviderOperations implements ProviderOperations {
	private final ProviderManager providerManager;
	private final AuthenticationJourneyRegistry authenticationJourneyRegistry;
	private final RegistrationJourneyRegistry registrationJourneyRegistry;
	private final MigrationJourneyRegistry migrationJourneyRegistry;
	private final EventManager eventManager;

	@Override
	public @Nullable ProfileResolution resolveProfile(@NotNull ProfileResolveContext context) {
		List<InternalProvider> sorted = sortedEnabledProviders();
		if (sorted.isEmpty())
			return resolveOfflineProfileFallback(context);

		for (InternalProvider provider : sorted) {
			Set<ProfileSubjectResolver> registered = provider.getProfileSubjectResolvers();
			if (registered == null || registered.isEmpty())
				continue;

			List<ProfileSubjectResolver> ordered = new ArrayList<>(registered);
			ordered.sort(Comparator.comparingInt(ProfileSubjectResolver::priority).reversed());

			for (ProfileSubjectResolver resolver : ordered) {
				if (resolver == null || !resolver.supports(context))
					continue;

				ProfileResolution resolution = resolver.resolve(context);
				if (resolution == null)
					continue;

				String providerId = resolution.getProviderId();
				String providerSubject = resolution.getProviderSubject();
				if (isBlank(providerId) || isBlank(providerSubject))
					continue;

				return resolution;
			}
		}

		return resolveOfflineProfileFallback(context);
	}

	@Override
	public @NotNull List<InternalProvider> eligibleProviders(
			@NotNull ScenarioContext context,
			@NotNull JourneyType flow
	) {
		return eligibleProviders(context, PipelineType.AUTHENTICATION, flow);
	}

	@Override
	public @NotNull List<InternalProvider> eligibleProviders(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow
	) {
		List<InternalProvider> providers = sortedEnabledProviders();
		if (providers.isEmpty())
			return List.of();

		List<InternalProvider> eligible = new ArrayList<>();
		for (InternalProvider provider : providers) {
			if (provider == null)
				continue;
			if (isEligible(context, provider, pipelineType, flow))
				eligible.add(provider);
		}
		return List.copyOf(eligible);
	}

	@Override
	public boolean isEligible(
			@NotNull ScenarioContext context,
			@NotNull InternalProvider provider,
			@NotNull JourneyType flow
	) {
		return isEligible(context, provider, PipelineType.AUTHENTICATION, flow);
	}

	@Override
	public boolean isEligible(
			@NotNull ScenarioContext context,
			@NotNull InternalProvider provider,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow
	) {
		if (provider.getState() != ProviderState.ENABLED)
			return false;

		ProviderDescriptor descriptor = provider.getDescriptor();
		if (descriptor == null || isBlank(descriptor.getId()))
			return false;

		if (!supportsJourney(context, provider, pipelineType, flow))
			return false;
		if (!resolversAllow(context, provider, flow))
			return false;

		ProviderEligibilityEvent event = new ProviderEligibilityEvent(context, provider, flow);
		eventManager.call(event);
		return !event.isCancelled();
	}

	private boolean supportsJourney(
			@NotNull ScenarioContext context,
			@NotNull InternalProvider provider,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow
	) {
		JourneyRegistry journeyRegistry = pipelineType == PipelineType.REGISTRATION
				? registrationJourneyRegistry
				: pipelineType == PipelineType.MIGRATION
						? migrationJourneyRegistry
						: authenticationJourneyRegistry;
		if (journeyRegistry == null) return false;

		JourneyPlan plan = journeyRegistry.resolvePlan(context, pipelineType, flow, provider.getDescriptor().getId());
		boolean hasProviderSteps = false;
		for (JourneyPlan.StageEntry stageEntry : plan.stages()) {
			if (stageEntry == null || !stageEntry.stage().providerStage())
				continue;

			if (stageEntry.steps().isEmpty())
				continue;

			hasProviderSteps = true;
			if (flow != JourneyType.SEAMLESS)
				continue;

			for (JourneyStep step : stageEntry.steps()) {
				if (step.getFlows().size() == 1 && step.getFlows().contains(JourneyType.INTERACTIVE))
					return false;
			}
		}

		return hasProviderSteps;
	}

	private boolean resolversAllow(
			@NotNull ScenarioContext context,
			@NotNull InternalProvider provider,
			@NotNull JourneyType flow
	) {
		Set<ProviderEligibilityResolver> resolvers = provider.getEligibilityResolvers();
		if (resolvers == null || resolvers.isEmpty())
			return true;

		for (ProviderEligibilityResolver resolver : resolvers) {
			if (!resolver.isEligible(context, provider, flow))
				return false;
		}

		return true;
	}

	private @NotNull List<InternalProvider> sortedEnabledProviders() {
		List<InternalProvider> providers = providerManager.getProviders();
		if (providers == null || providers.isEmpty())
			return List.of();

		List<InternalProvider> sorted = new ArrayList<>(providers.size());
		for (InternalProvider provider : providers) {
			if (provider == null || provider.getState() != ProviderState.ENABLED)
				continue;
			if (provider.getDescriptor() == null || isBlank(provider.getDescriptor().getId()))
				continue;
			sorted.add(provider);
		}

		sorted.sort(Comparator.comparingInt(InternalProvider::getPriority)
				.reversed()
				.thenComparing(this::resolveId, String.CASE_INSENSITIVE_ORDER));
		return sorted;
	}

	private @Nullable ProfileResolution resolveOfflineProfileFallback(@NotNull ProfileResolveContext context) {
		InternalProvider provider = providerManager.findProvider(ProviderCapability.OFFLINE_MODE);
		if (provider == null || provider.getDescriptor() == null)
			return null;

		String username = context.getUsername();
		if (username == null || username.isBlank())
			return null;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid == null)
			return null;

		return ProfileResolution.builder()
				.providerId(provider.getDescriptor().getId())
				.providerSubject(offlineUuid.toString())
				.build();
	}

	private @NotNull String resolveId(@NotNull InternalProvider provider) {
		ProviderDescriptor descriptor = provider.getDescriptor();
		String id = descriptor != null ? descriptor.getId() : null;
		return id != null ? id : "";
	}

	private boolean isBlank(@Nullable String value) {
		return value == null || value.isBlank();
	}
}
