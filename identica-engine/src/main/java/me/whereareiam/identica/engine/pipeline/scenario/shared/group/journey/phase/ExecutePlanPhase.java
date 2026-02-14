package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.JourneyState;
import me.whereareiam.identica.event.auth.provider.ProviderSelectedEvent;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.step.StepFinishedEvent;
import me.whereareiam.identica.event.step.StepPrepareEvent;
import me.whereareiam.identica.event.step.StepStartedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.journey.JourneyOverrideItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionBlock;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionPlan;
import me.whereareiam.identica.type.pipeline.journey.JourneyExecutionPolicy;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionStage;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExecutePlanPhase implements PipelinePhase<JourneyState> {
	private final IdentityService identityService;
	private final EventManager eventManager;
	private final Provider<Settings> settingsProvider;
	private final Provider<Messages> messagesProvider;
	private final ProviderManager providerManager;
	private final PipelineStateStore pipelineStateStore;

	@Override
	public @NotNull String id() {
		return "execute-plan";
	}

	@Override
	public int order() {
		return 500;
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
		if (state.getResult() != null) {
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		ScenarioContext context = state.getContext();
		PipelineType pipelineType = pipelineState.getPipelineType();
		JourneyType flow = state.getFlow();
		JourneyExecutionPlan plan = state.getExecutionPlan();
		if (context == null || pipelineType == null || flow == null) {
			state.setResult(PipelineResult.failed(journeyMissingContextMessage(pipelineState)));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		if (plan == null) {
			state.setResult(PipelineResult.failed(journeyMissingPlanMessage(pipelineState)));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		JourneyStateItem pending = state.getPending();
		PipelineResult result = executePlan(pipelineState, context, pipelineType, flow, pending, plan);
		state.setResult(result != null ? result : PipelineResult.complete());
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @Nullable PipelineResult executePlan(
			@NotNull PipelineState pipelineState,
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable JourneyStateItem pending,
			@NotNull JourneyExecutionPlan plan
	) {
		List<JourneyExecutionBlock> blocks = plan.blocks();
		String pendingProviderId = resolvePendingProviderId(context, pending);
		int startIndex = resolveStartIndex(blocks, pending, pendingProviderId, context);

		for (int index = startIndex; index < blocks.size(); index++) {
			JourneyExecutionBlock block = blocks.get(index);
			if (block == null) continue;

			if (block.policy() == JourneyExecutionPolicy.FALLBACK) {
				BlockRegion region = collectFallbackRegion(blocks, index);
				PipelineResult fallbackResult = executeFallbackBlocks(
						pipelineState,
						context,
						pipelineType,
						flow,
						pending,
						pendingProviderId,
						region.blocks()
				);
				if (fallbackResult != null)
					return fallbackResult;

				context = currentScenario(pipelineState, pipelineType, context);
				index = region.endIndex();
				continue;
			}

			PipelineResult blockResult = executeBlock(
					pipelineState,
					context,
					pipelineType,
					flow,
					pending,
					pendingProviderId,
					block
			);
			if (blockResult != null)
				return blockResult;

			context = currentScenario(pipelineState, pipelineType, context);
		}

		return null;
	}

	private @Nullable PipelineResult executeFallbackBlocks(
			@NotNull PipelineState pipelineState,
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable JourneyStateItem pending,
			@Nullable String pendingProviderId,
			@NotNull List<JourneyExecutionBlock> blocks
	) {
		boolean anySuccess = false;

		for (JourneyExecutionBlock block : blocks) {
			if (block == null) continue;
			context = currentScenario(pipelineState, pipelineType, context);

			PipelineResult blockResult = executeBlock(
					pipelineState,
					context,
					pipelineType,
					flow,
					pending,
					pendingProviderId,
					block
			);
			if (blockResult == null) {
				anySuccess = true;
				break;
			}

			PipelineStatus status = blockResult.getStatus();
			if (status == PipelineStatus.WAITING || status == PipelineStatus.REQUIRE_RECONNECT)
				return blockResult;

			if (status == PipelineStatus.COMPLETE) {
				anySuccess = true;
				break;
			}

			if (status == PipelineStatus.FAILED
					|| status == PipelineStatus.DENIED
					|| status == PipelineStatus.NO_PENDING) {
				if (!allowFallback(block.stages()))
					return blockResult;
			}
		}

		if (!anySuccess)
			return PipelineResult.failed(journeyNoCompletionMessage());
		return null;
	}

	private @Nullable PipelineResult executeBlock(
			@NotNull PipelineState pipelineState,
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable JourneyStateItem pending,
			@Nullable String pendingProviderId,
			@NotNull JourneyExecutionBlock block
	) {
		String effectiveProviderId = resolveEffectiveProviderId(block, context);
		String blockProviderId = block.providerId();
		if (blockProviderId != null && !blockProviderId.isBlank()) {
			applyProviderContext(context, blockProviderId);
		}

		InternalProvider provider = resolveProvider(effectiveProviderId);
		if (blockProviderId != null && provider != null) {
			eventManager.call(new ProviderSelectedEvent(context, provider, flow));
		}

		return executeStageEntries(
				pipelineState,
				context,
				pipelineType,
				flow,
				pending,
				pendingProviderId,
				block.stages(),
				effectiveProviderId,
				provider
		);
	}

	private boolean allowFallback(@NotNull List<JourneyExecutionStage> stages) {
		for (JourneyExecutionStage entry : stages) {
			if (entry == null) continue;
			if (!entry.stage().isAllowFallback()) return false;
		}
		return true;
	}

	private @Nullable PipelineResult executeStageEntries(
			@NotNull PipelineState pipelineState,
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable JourneyStateItem pending,
			@Nullable String pendingProviderId,
			@NotNull List<JourneyExecutionStage> stages,
			@Nullable String providerId,
			@Nullable InternalProvider provider
	) {
		int startStageIndex = 0;
		if (pending != null
				&& pending.getStageId() != null
				&& !pending.getStageId().isBlank()
				&& (providerId == null || providerId.isBlank() || matchesProvider(pendingProviderId, providerId))) {
			for (int index = 0; index < stages.size(); index++) {
				JourneyExecutionStage candidate = stages.get(index);
				if (candidate == null) continue;
				if (candidate.stage().getId().equalsIgnoreCase(pending.getStageId())) {
					startStageIndex = index;
					break;
				}
			}
		}

		for (int stageIndex = startStageIndex; stageIndex < stages.size(); stageIndex++) {
			JourneyExecutionStage entry = stages.get(stageIndex);
			if (entry == null) continue;

			JourneyStage stage = entry.stage();
			int startIndex = 0;
			if (pending != null
					&& pending.getStageId() != null
					&& pending.getStageId().equalsIgnoreCase(stage.getId())
					&& (providerId == null || providerId.isBlank() || matchesProvider(pendingProviderId, providerId))) {
				startIndex = Math.max(0, pending.getStepIndex());
			}

			boolean completedStage = false;
			List<JourneyStep> steps = entry.steps();
			for (int index = startIndex; index < steps.size(); index++) {
				JourneyStep journeyStep = steps.get(index);
				StepResult stepResult = executeStep(
						context,
						pipelineType,
						flow,
						stage,
						journeyStep,
						provider
				);

				if (stepResult.getUpdatedContext() != null) {
					pipelineState.setScenario(stepResult.getUpdatedContext());
					context = stepResult.getUpdatedContext();
				}

				PipelineStatus status = PipelineResult.fromStepResult(stepResult).getStatus();
				if (status == PipelineStatus.CONTINUE)
					continue;
				if (status == PipelineStatus.COMPLETE) {
					completedStage = true;
					if (stage.isUsesCompletionResult())
						return PipelineResult.fromStepResult(stepResult);
					continue;
				}

				if (status == PipelineStatus.WAITING || status == PipelineStatus.REQUIRE_RECONNECT)
					persistPending(pipelineState, context, flow, stage.getId(), index);

				return PipelineResult.fromStepResult(stepResult);
			}

			if (stage.isRequireCompletion() && !completedStage)
				return PipelineResult.failed(journeyNoCompletionMessage());
		}
		return null;
	}

	private @NotNull StepResult executeStep(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@NotNull JourneyStage stage,
			@NotNull JourneyStep journeyStep,
			@Nullable InternalProvider provider
	) {
		if (flow == JourneyType.INTERACTIVE
				&& requiresOnlinePresence(journeyStep)
				&& !isOnline(context)) {
			return StepResult.waiting("");
		}

		if (!journeyStep.getStep().shouldExecute(context))
			return StepResult.proceed(context);

		eventManager.call(new StepPrepareEvent(
				provider != null ? provider.getProvider() : null,
				journeyStep.getStep(),
				context,
				flow,
				stage.getType()
		));
		eventManager.call(new StepStartedEvent(
				provider != null ? provider.getProvider() : null,
				journeyStep.getStep(),
				context
		));

		StepResult result;
		try {
			result = journeyStep.getStep().execute(context).join();
			if (result == null)
				result = StepResult.failed(journeyStepNoStatusMessage());
		} catch (Exception exception) {
			result = StepResult.failed(failureMessage(pipelineType));
		}

		eventManager.call(new StepFinishedEvent(
				provider != null ? provider.getProvider() : null,
				journeyStep.getStep(),
				context,
				result,
				stage.getType()
		));
		return result;
	}

	private int resolveStartIndex(
			@NotNull List<JourneyExecutionBlock> blocks,
			@Nullable JourneyStateItem pending,
			@Nullable String pendingProviderId,
			@NotNull ScenarioContext context
	) {
		if (pending == null || pending.getStageId() == null || pending.getStageId().isBlank())
			return 0;

		String stageId = pending.getStageId();
		for (int index = 0; index < blocks.size(); index++) {
			JourneyExecutionBlock block = blocks.get(index);
			if (block == null) continue;

			String effectiveProviderId = resolveEffectiveProviderId(block, context);
			if (effectiveProviderId != null && !effectiveProviderId.isBlank()) {
				if (!matchesProvider(pendingProviderId, effectiveProviderId))
					continue;
			}

			for (JourneyExecutionStage entry : block.stages()) {
				if (entry == null) continue;
				if (entry.stage().getId().equalsIgnoreCase(stageId))
					return index;
			}
		}
		return 0;
	}

	private @NotNull BlockRegion collectFallbackRegion(
			@NotNull List<JourneyExecutionBlock> blocks,
			int startIndex
	) {
		JourneyExecutionBlock seed = blocks.get(startIndex);
		if (seed == null) return new BlockRegion(List.of(), startIndex);

		String groupId = seed.groupId();
		int endIndex = startIndex;
		List<JourneyExecutionBlock> grouped = new ArrayList<>();
		for (int index = startIndex; index < blocks.size(); index++) {
			JourneyExecutionBlock block = blocks.get(index);
			if (block == null) break;
			if (block.policy() != JourneyExecutionPolicy.FALLBACK)
				break;
			if (!block.groupId().equalsIgnoreCase(groupId))
				break;
			grouped.add(block);
			endIndex = index;
		}
		return new BlockRegion(List.copyOf(grouped), endIndex);
	}

	private void persistPending(
			@NotNull PipelineState pipelineState,
			@NotNull ScenarioContext context,
			@NotNull JourneyType flow,
			@NotNull String stageId,
			int stepIndex
	) {
		JourneyType resolvedFlow = flow;
		String resolvedStageId = stageId;
		int resolvedStepIndex = stepIndex;
		boolean clearProvider = false;
		String overrideProviderId = null;

		PipelineStateReference reference = PipelineStateReference.from(context);
		if (!reference.isEmpty()) {
			PipelineState stored = pipelineStateStore.find(reference).orElse(null);
			if (stored != null) {
				JourneyOverrideItem override = stored.item(JourneyOverrideItem.class).orElse(null);
				if (override != null) {
					if (override.getFlow() != null)
						resolvedFlow = override.getFlow();
					if (override.getStageId() != null && !override.getStageId().isBlank())
						resolvedStageId = override.getStageId();
					if (override.getStepIndex() >= 0)
						resolvedStepIndex = override.getStepIndex();

					clearProvider = override.isClearProvider();
					overrideProviderId = override.getProviderId();
				}
			}
		}

		if (clearProvider) {
			context.setProvider(null);
			pipelineState.setScenario(context);
		} else if (overrideProviderId != null && !overrideProviderId.isBlank()) {
			applyProviderContext(context, overrideProviderId);
			pipelineState.setScenario(context);
		}

		long ttlMs = scenarioSettings(pipelineState.getPipelineType()).pipelineTtlMillis();
		pipelineState.putItem(new JourneyStateItem(resolvedFlow, resolvedStageId, resolvedStepIndex), ttlMs);
	}

	private boolean isOnline(@NotNull ScenarioContext context) {
		UUID connectionId = context.getConnectionUniqueId();
		if (connectionId != null && identityService.find(connectionId).isPresent())
			return true;

		String username = context.getUsername();
		if (username != null && !username.isBlank())
			return identityService.find(username).isPresent();

		return false;
	}

	private boolean requiresOnlinePresence(@NotNull JourneyStep step) {
		Set<JourneyType> flows = step.getFlows();
		if (flows.isEmpty()) return true;

		return flows.contains(JourneyType.INTERACTIVE);
	}

	private @NotNull Settings.Scenario scenarioSettings(@Nullable PipelineType pipelineType) {
		Settings.Connection connection = settingsProvider.get().getConnection();
		if (pipelineType == PipelineType.REGISTRATION)
			return connection.getRegistration();
		if (pipelineType == PipelineType.MIGRATION)
			return connection.getMigration();

		return connection.getAuthentication();
	}

	private void applyProviderContext(@NotNull ScenarioContext context, @NotNull String providerId) {
		me.whereareiam.identica.model.provider.ProviderContext provider = context.getProvider();
		String username = context.getUsername() != null ? context.getUsername() : "";
		if (provider == null) {
			context.setProvider(me.whereareiam.identica.model.provider.ProviderContext.builder()
					.providerId(providerId)
					.providerUsername(username)
					.build());
			return;
		}

		provider.setProviderId(providerId);
		if (provider.getProviderUsername().isBlank())
			provider.setProviderUsername(username);
	}

	private boolean matchesProvider(@Nullable String expected, @Nullable String actual) {
		if (expected == null || expected.isBlank())
			return actual == null || actual.isBlank();

		if (actual == null || actual.isBlank()) return false;
		return expected.equalsIgnoreCase(actual);
	}

	private @Nullable String resolveEffectiveProviderId(
			@NotNull JourneyExecutionBlock block,
			@NotNull ScenarioContext context
	) {
		String providerId = block.providerId();
		if (providerId != null && !providerId.isBlank())
			return providerId;

		ProviderContext provider = context.getProvider();
		if (provider == null) return null;

		String contextProviderId = provider.getProviderId();
		return contextProviderId != null && !contextProviderId.isBlank()
				? contextProviderId
				: null;
	}

	private @Nullable String resolvePendingProviderId(
			@NotNull ScenarioContext context,
			@Nullable JourneyStateItem pending
	) {
		if (pending == null) return null;

		ProviderContext provider = context.getProvider();
		if (provider == null) return null;

		String providerId = provider.getProviderId();
		if (providerId == null || providerId.isBlank()) return null;

		return providerId;
	}

	private @NotNull ScenarioContext currentScenario(
			@NotNull PipelineState pipelineState,
			@NotNull PipelineType pipelineType,
			@NotNull ScenarioContext fallback
	) {
		ScenarioContext current = pipelineState.getScenario(pipelineType);
		return current != null ? current : fallback;
	}

	private @NotNull String journeyNoCompletionMessage() {
		return String.join("\n", messagesProvider.get()
				.getConnection()
				.getJourney()
				.getStage()
				.getNoCompletion());
	}

	private @NotNull String journeyStepNoStatusMessage() {
		return String.join("\n", messagesProvider.get()
				.getConnection()
				.getJourney()
				.getStep()
				.getNoStatus());
	}

	private @NotNull String journeyMissingContextMessage(@NotNull PipelineState pipelineState) {
		Messages.Connection.Scenario.Errors errors = resolveScenarioErrors(pipelineState);
		return String.join("\n", errors.getJourneyMissingContext());
	}

	private @NotNull String journeyMissingPlanMessage(@NotNull PipelineState pipelineState) {
		Messages.Connection.Scenario.Errors errors = resolveScenarioErrors(pipelineState);
		return String.join("\n", errors.getJourneyMissingPlan());
	}

	private @NotNull Messages.Connection.Scenario.Errors resolveScenarioErrors(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (pipelineType == PipelineType.REGISTRATION)
			return connection.getRegistration().getErrors();
		if (pipelineType == PipelineType.MIGRATION)
			return connection.getMigration().getErrors();

		return connection.getAuthentication().getErrors();
	}

	private @NotNull String failureMessage(@Nullable PipelineType pipelineType) {
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (pipelineType == PipelineType.REGISTRATION)
			return String.join("\n", connection.getRegistration().getRegistrationFailed());
		if (pipelineType == PipelineType.MIGRATION)
			return String.join("\n", connection.getMigration().getMigrationFailed());

		return String.join("\n", connection.getAuthentication().getAuthenticationFailed());
	}

	private @Nullable InternalProvider resolveProvider(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;

		List<InternalProvider> providers = providerManager.getProviders();
		if (providers == null) return null;

		for (InternalProvider provider : providers) {
			if (provider == null || provider.getDescriptor() == null)
				continue;

			String id = provider.getDescriptor().getId();
			if (id.equalsIgnoreCase(providerId))
				return provider;
		}
		return null;
	}

	private record BlockRegion(@NotNull List<JourneyExecutionBlock> blocks, int endIndex) {
	}
}
