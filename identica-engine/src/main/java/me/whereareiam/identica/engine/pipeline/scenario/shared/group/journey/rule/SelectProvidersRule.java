package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.rule;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyPendingState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionBlock;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionPlan;
import me.whereareiam.identica.type.pipeline.journey.JourneyExecutionPolicy;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionStage;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRule;
import me.whereareiam.identica.model.pipeline.journey.JourneyRuleContext;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRuleScope;
import me.whereareiam.identica.pipeline.journey.registry.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.pipeline.journey.registry.JourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.RegistrationJourneyRegistry;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SelectProvidersRule implements JourneyRule {
	private final ProviderOperations providerOperations;
	private final AuthenticationJourneyRegistry authenticationJourneyRegistry;
	private final RegistrationJourneyRegistry registrationJourneyRegistry;

	@Override
	public @NotNull String id() {
		return "select-providers";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull JourneyRuleScope scope() {
		return JourneyRuleScope.forStageType(StageType.PROVIDER);
	}

	@Override
	public @NotNull JourneyExecutionPlan apply(
			@NotNull JourneyRuleContext ctx,
			@NotNull JourneyExecutionPlan current
	) {
		if (current.blocks().isEmpty())
			return current;

		ScenarioContext context = ctx.context();
		PipelineType pipelineType = ctx.pipelineType();
		JourneyType flow = ctx.flow();
		JourneyPendingState pending = ctx.pending();

		List<InternalProvider> eligibleProviders = new ArrayList<>(
				providerOperations.eligibleProviders(context, pipelineType, flow)
		);
		String pendingProviderId = pending != null && context.getProvider() != null
				? context.getProvider().getProviderId()
				: null;
		List<String> orderedProviderIds = orderedProviderIds(eligibleProviders, pendingProviderId);

		if (orderedProviderIds.isEmpty())
			return current;

		JourneyRegistry registry = resolveRegistry(pipelineType);
		List<JourneyExecutionBlock> resolvedBlocks = new ArrayList<>();
		for (String providerId : orderedProviderIds) {
			if (providerId == null || providerId.isBlank())
				continue;
			JourneyPlan providerPlan = registry.resolvePlan(context, pipelineType, flow, providerId);
			List<JourneyExecutionStage> providerStages = new ArrayList<>();
			for (JourneyPlan.StageEntry entry : providerPlan.stages()) {
				if (entry == null)
					continue;
				if (!entry.stage().providerStage())
					continue;
				providerStages.add(new JourneyExecutionStage(entry.stage(), entry.steps()));
			}
			if (providerStages.isEmpty())
				continue;
			resolvedBlocks.add(new JourneyExecutionBlock(
					id(),
					JourneyExecutionPolicy.FALLBACK,
					providerId,
					providerStages
			));
		}

		if (resolvedBlocks.isEmpty())
			return current;

		return new JourneyExecutionPlan(resolvedBlocks);
	}

	private @NotNull JourneyRegistry resolveRegistry(@NotNull PipelineType pipelineType) {
		return pipelineType == PipelineType.REGISTRATION
				? registrationJourneyRegistry
				: authenticationJourneyRegistry;
	}

	private @NotNull List<String> orderedProviderIds(
			@NotNull List<InternalProvider> eligibleProviders,
			@Nullable String pendingProviderId
	) {
		List<String> providerIds = new ArrayList<>();
		for (InternalProvider provider : eligibleProviders) {
			String id = providerId(provider);
			if (id.isBlank())
				continue;
			if (indexOfProvider(providerIds, id) < 0)
				providerIds.add(id);
		}

		providerIds.sort(String.CASE_INSENSITIVE_ORDER);

		if (pendingProviderId == null || pendingProviderId.isBlank())
			return providerIds;

		int existingIndex = indexOfProvider(providerIds, pendingProviderId);
		if (existingIndex >= 0) providerIds.remove(existingIndex);
		providerIds.addFirst(pendingProviderId);

		return providerIds;
	}

	private int indexOfProvider(@NotNull List<String> providerIds, @NotNull String pendingProviderId) {
		for (int index = 0; index < providerIds.size(); index++) {
			String candidate = providerIds.get(index);
			if (candidate != null && candidate.equalsIgnoreCase(pendingProviderId))
				return index;
		}
		return -1;
	}

	private @NotNull String providerId(@Nullable InternalProvider provider) {
		if (provider == null || provider.getDescriptor() == null)
			return "";

		return provider.getDescriptor().getId();
	}
}
