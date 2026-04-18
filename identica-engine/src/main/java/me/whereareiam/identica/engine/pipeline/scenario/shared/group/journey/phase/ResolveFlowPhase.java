package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.JourneyState;
import me.whereareiam.identica.event.pipeline.scenario.authentication.AuthenticationScenarioStartedEvent;
import me.whereareiam.identica.event.pipeline.scenario.registration.RegistrationScenarioStartedEvent;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.pipeline.journey.registry.type.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.JourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.type.RegistrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.type.MigrationJourneyRegistry;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolveFlowPhase implements PipelinePhase<JourneyState> {
	private final ProviderOperations providerOperations;
	private final AuthenticationJourneyRegistry authenticationJourneyRegistry;
	private final RegistrationJourneyRegistry registrationJourneyRegistry;
	private final MigrationJourneyRegistry migrationJourneyRegistry;
	private final Provider<Settings> settingsProvider;

	@Override
	public @NotNull String id() {
		return "resolve-flow";
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
		if (state.getResult() != null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ScenarioContext context = state.getContext();
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (context == null || pipelineType == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		JourneyType flow = resolveFlow(context, pipelineType, state.getPending());
		state.setFlow(flow);
		fireScenarioStarted(context, flow, pipelineType, pipelineState);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private void fireScenarioStarted(
			@NotNull ScenarioContext context,
			@NotNull JourneyType flow,
			@NotNull PipelineType pipelineType,
			@NotNull PipelineState pipelineState
	) {
		boolean resumed = resolveResumed(pipelineType, pipelineState);
		if (context instanceof me.whereareiam.identica.model.auth.AuthContext authContext) {
			EventUtil.callEvent(new AuthenticationScenarioStartedEvent(authContext, flow, resumed));
		}
		if (context instanceof me.whereareiam.identica.model.registration.RegistrationContext registrationContext) {
			EventUtil.callEvent(new RegistrationScenarioStartedEvent(registrationContext, flow, resumed));
		}
	}

	private boolean resolveResumed(
			@NotNull PipelineType pipelineType,
			@NotNull PipelineState pipelineState
	) {
		if (pipelineType == PipelineType.AUTHENTICATION) {
			me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem identity =
					pipelineState.item(me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem.class)
							.orElse(null);
			return identity != null && identity.isResumed();
		}
		if (pipelineType == PipelineType.REGISTRATION) {
			me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityMetaItem identity =
					pipelineState.item(me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityMetaItem.class)
							.orElse(null);
			return identity != null && identity.isResumed();
		}
		if (pipelineType == PipelineType.MIGRATION) {
			me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem identity =
					pipelineState.item(me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem.class)
							.orElse(null);
			return identity != null && identity.isResumed();
		}
		return false;
	}

	private @NotNull JourneyType resolveFlow(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			JourneyStateItem pending
	) {
		if (pending != null && pending.getFlow() != null)
			return pending.getFlow();

		JourneyType preferred = preferredFlow(pipelineType);
		if (isViable(context, pipelineType, preferred))
			return preferred;

		JourneyType fallback = preferred == JourneyType.SEAMLESS
				? JourneyType.INTERACTIVE
				: JourneyType.SEAMLESS;
		if (isViable(context, pipelineType, fallback))
			return fallback;
		return preferred;
	}

	private boolean isViable(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow
	) {
		JourneyRegistry registry = resolveRegistry(pipelineType);
		JourneyPlan basePlan = registry.resolvePlan(context, pipelineType, flow, null);
		for (JourneyPlan.StageEntry entry : basePlan.stages()) {
			if (entry == null)
				continue;
			if (entry.stage().providerStage())
				continue;
			if (!entry.steps().isEmpty())
				return true;
		}

		Set<String> checkedProviders = new HashSet<>();
		List<InternalProvider> eligibleProviders = providerOperations.eligibleProviders(context, pipelineType, flow);
		for (InternalProvider eligibleProvider : eligibleProviders) {
			String providerId = providerId(eligibleProvider);
			if (providerId.isBlank())
				continue;
			String normalized = providerId.toLowerCase();
			if (!checkedProviders.add(normalized))
				continue;
			if (hasProviderSteps(registry.resolvePlan(context, pipelineType, flow, providerId)))
				return true;
		}

		String selectedProviderId = context.getProvider() != null ? context.getProvider().getProviderId() : null;
		if (selectedProviderId != null && !selectedProviderId.isBlank()) {
			String normalized = selectedProviderId.toLowerCase();
			if (!checkedProviders.contains(normalized))
				return hasProviderSteps(registry.resolvePlan(context, pipelineType, flow, selectedProviderId));
		}

		return false;
	}

	private boolean hasProviderSteps(@NotNull JourneyPlan plan) {
		for (JourneyPlan.StageEntry entry : plan.stages()) {
			if (entry == null || !entry.stage().providerStage())
				continue;
			if (!entry.steps().isEmpty())
				return true;
		}
		return false;
	}

	private @NotNull JourneyRegistry resolveRegistry(@NotNull PipelineType pipelineType) {
		if (pipelineType == PipelineType.REGISTRATION)
			return registrationJourneyRegistry;
		if (pipelineType == PipelineType.MIGRATION)
			return migrationJourneyRegistry;
		return authenticationJourneyRegistry;
	}

	private @NotNull JourneyType preferredFlow(@NotNull PipelineType pipelineType) {
		Settings.Connection connection = settingsProvider.get().getConnection();
		Settings.Scenario scenario = pipelineType == PipelineType.REGISTRATION
				? connection.getRegistration()
				: pipelineType == PipelineType.MIGRATION
						? connection.getMigration()
						: connection.getAuthentication();
		return scenario.getFlow();
	}

	private @NotNull String providerId(InternalProvider provider) {
		if (provider == null || provider.getDescriptor() == null)
			return "";

		return provider.getDescriptor().getId();
	}
}
