package me.whereareiam.identica.engine.pipeline.scenario.base.journey.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Engine;
import me.whereareiam.identica.model.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.scenario.base.JourneyState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.registry.JourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.type.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.type.MigrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.type.RegistrationJourneyRegistry;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.JourneyPolicy;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolveJourneyModePhase implements PipelinePhase<JourneyState> {
	private final ProviderOperations providerOperations;
	private final AuthenticationJourneyRegistry authenticationJourneyRegistry;
	private final RegistrationJourneyRegistry registrationJourneyRegistry;
	private final MigrationJourneyRegistry migrationJourneyRegistry;
	private final Provider<Engine> engineProvider;

	@Override
	public @NotNull String id() {
		return "resolve-journey-mode";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<JourneyState> stateType() {
		return JourneyState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<JourneyState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull JourneyState state
	) {
		if (state.getResult() != null) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ScenarioContext context = state.getContext();
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (context == null || pipelineType == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		JourneyMode journeyMode = resolveJourneyMode(context, pipelineType, state.getPending());
		state.setJourneyMode(journeyMode);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull JourneyMode resolveJourneyMode(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			JourneyStateItem pending
	) {
		if (pending != null && pending.getJourneyMode() != null)
			return pending.getJourneyMode();

		Engine.Scenario scenario = scenario(pipelineType);
		JourneyMode preferred = scenario.getJourneyMode();
		if (isViable(context, pipelineType, preferred)) return preferred;

		if (scenario.getJourneyPolicy() != JourneyPolicy.STRICT) {
			for (JourneyMode candidate : JourneyMode.values()) {
				if (candidate == preferred) continue;
				if (isViable(context, pipelineType, candidate))
					return candidate;
			}
		}
		return preferred;
	}

	private boolean isViable(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyMode journeyMode
	) {
		JourneyRegistry registry = resolveRegistry(pipelineType);
		JourneyPlan basePlan = registry.resolvePlan(context, pipelineType, journeyMode, null);
		for (JourneyPlan.StageEntry entry : basePlan.stages()) {
			if (entry == null) continue;
			if (entry.stage().providerStage()) continue;
			if (!entry.steps().isEmpty()) return true;
		}

		Set<String> checkedProviders = new HashSet<>();
		List<InternalProvider> eligibleProviders = providerOperations.eligibleProviders(context, pipelineType, journeyMode);
		for (InternalProvider eligibleProvider : eligibleProviders) {
			String providerId = providerId(eligibleProvider);
			if (providerId.isBlank()) continue;
			String normalized = providerId.toLowerCase();
			if (!checkedProviders.add(normalized)) continue;
			if (hasProviderSteps(registry.resolvePlan(context, pipelineType, journeyMode, providerId)))
				return true;
		}

		String selectedProviderId = context.getProvider() != null ? context.getProvider().getProviderId() : null;
		if (selectedProviderId != null && !selectedProviderId.isBlank()) {
			String normalized = selectedProviderId.toLowerCase();
			if (!checkedProviders.contains(normalized))
				return hasProviderSteps(registry.resolvePlan(context, pipelineType, journeyMode, selectedProviderId));
		}

		return false;
	}

	private boolean hasProviderSteps(@NotNull JourneyPlan plan) {
		for (JourneyPlan.StageEntry entry : plan.stages()) {
			if (entry == null || !entry.stage().providerStage()) continue;
			if (!entry.steps().isEmpty()) return true;
		}
		return false;
	}

	private @NotNull JourneyRegistry resolveRegistry(@NotNull PipelineType pipelineType) {
		if (pipelineType == PipelineType.REGISTRATION) return registrationJourneyRegistry;
		if (pipelineType == PipelineType.MIGRATION) return migrationJourneyRegistry;
		return authenticationJourneyRegistry;
	}

	private @NotNull Engine.Scenario scenario(@NotNull PipelineType pipelineType) {
		Engine.Scenarios scenarios = engineProvider.get().getScenarios();
		return pipelineType == PipelineType.REGISTRATION
				? scenarios.getRegistration()
				: pipelineType == PipelineType.MIGRATION
						? scenarios.getMigration()
						: scenarios.getAuthentication();
	}

	private @NotNull String providerId(InternalProvider provider) {
		if (provider == null || provider.getDescriptor() == null) return "";
		return provider.getDescriptor().getId();
	}
}
