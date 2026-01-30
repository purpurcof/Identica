package me.whereareiam.identica.common.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.IdenticaKeys;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.common.auth.stage.StageOutcome;
import me.whereareiam.identica.common.auth.stage.runner.GlobalStageRunner;
import me.whereareiam.identica.common.auth.stage.runner.ProviderStageRunner;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.flow.AuthFlowFinishedEvent;
import me.whereareiam.identica.event.auth.flow.AuthFlowStartedEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.connection.ConnectionState;
import me.whereareiam.identica.model.connection.FlowState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.registry.ConnectionStateRegistry;
import me.whereareiam.identica.stage.PendingStage;
import me.whereareiam.identica.stage.StepStage;
import me.whereareiam.identica.stage.StepStageRegistry;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FlowCoordinator {
	private final ProviderEligibilityService eligibilityService;
	private final StepStageRegistry stageRegistry;
	private final Provider<Messages> messagesProvider;
	private final Provider<Settings> settingsProvider;
	private final EventManager eventManager;
	private final GlobalStageRunner globalStageRunner;
	private final ProviderStageRunner providerStageRunner;
	private final ConnectionStateRegistry connectionStateRegistry;

	public CompletableFuture<StepResult> authenticate(@Nullable AuthContext context) {
		if (context == null) {
			return CompletableFuture.completedFuture(StepResult.failed(
					joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed())
			));
		}

		storeContext(context);

		UUID connectionId = context.getConnectionUniqueId();
		if (connectionId != null && connectionStateRegistry.hasPending(connectionId)) {
			return CompletableFuture.completedFuture(StepResult.waiting(""));
		}

		AuthFlowType flow = resolveFlow(context);
		if (flow == null)
			return CompletableFuture.completedFuture(providerStageRunner.noProvidersResult());

		context.put(IdenticaKeys.CURRENT_FLOW, flow);
		eventManager.call(new AuthFlowStartedEvent(context, flow));

		List<StepStage> stages = stageRegistry.resolve(context, flow);
		if (flow == AuthFlowType.INTERACTIVE) {
			int stageIndex = findFirstStageIndex(stages, context, flow);
			if (stageIndex >= 0) {
				StepStage stage = stages.get(stageIndex);
				List<AuthenticationStep> steps = stage.steps(context, flow, null);
				PendingStage pendingStage = new PendingStage(null, steps, -1);
				storeWaiting(context, flow, stages, stageIndex, pendingStage, null);
				return CompletableFuture.completedFuture(StepResult.waiting(""));
			}
		}
		CompletableFuture<StepResult> result = executeStages(context, flow, stages, 0, null);

		return result.thenApply(stepResult -> finalizeFlow(context, flow, stepResult));
	}

	public CompletableFuture<StepResult> resume(
			@Nullable UUID connectionUniqueId,
			@Nullable Consumer<AuthContext> contextUpdater
	) {
		if (connectionUniqueId == null) {
			return CompletableFuture.completedFuture(StepResult.failed(
					joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed())
			));
		}

		FlowState waiting = connectionStateRegistry.find(connectionUniqueId)
				.flatMap(ConnectionState::consumeFlowState)
				.orElse(null);
		if (waiting == null)
			waiting = connectionStateRegistry.consumePending(connectionUniqueId).orElse(null);
		if (waiting == null)
			return CompletableFuture.completedFuture(StepResult.noPending());

		if (contextUpdater != null)
			contextUpdater.accept(waiting.getContext());

		AuthFlowType flow = waiting.getFlow();
		AuthContext context = waiting.getContext();
		context.put(IdenticaKeys.CURRENT_FLOW, flow);

		storeContext(context);
		CompletableFuture<StepResult> result = resumeFromState(waiting);
		return result.thenApply(stepResult -> finalizeFlow(context, flow, stepResult));
	}

	public boolean hasPending(@Nullable UUID connectionUniqueId) {
		if (connectionUniqueId == null) return false;
		return connectionStateRegistry.hasPending(connectionUniqueId);
	}

	public boolean clearPending(@Nullable UUID connectionUniqueId) {
		if (connectionUniqueId == null)
			return false;

		boolean removed = connectionStateRegistry.find(connectionUniqueId)
				.map(ConnectionState::clearFlowState)
				.orElse(false);
		connectionStateRegistry.clearPending(connectionUniqueId);
		eventManager.call(new me.whereareiam.identica.event.auth.AuthPendingClearedEvent(connectionUniqueId, removed));

		return removed;
	}

	private CompletableFuture<StepResult> resumeFromState(@NotNull FlowState waiting) {
		List<StepStage> stages = waiting.getStages();
		int stageIndex = waiting.getStageIndex();
		if (stageIndex < 0 || stageIndex >= stages.size())
			return CompletableFuture.completedFuture(StepResult.noPending());

		StepStage stage = stages.get(stageIndex);
		if (stage == null)
			return CompletableFuture.completedFuture(StepResult.noPending());

		PendingStage pendingStage = waiting.getPendingStage();
		CompletableFuture<StageOutcome> outcome = (stage.providerStage()
				? providerStageRunner.resume(stage, waiting.getContext(), waiting.getFlow(), waiting.getCompletionResult(), pendingStage)
				: globalStageRunner.resume(stage, waiting.getContext(), waiting.getFlow(), waiting.getCompletionResult(), pendingStage))
				.toCompletableFuture();

		return outcome.thenCompose(stageOutcome -> applyOutcome(stageOutcome, waiting.getFlow(), stages, stageIndex));
	}

	private CompletableFuture<StepResult> executeStages(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@NotNull List<StepStage> stages,
			int stageIndex,
			@Nullable StepResult completionResult
	) {
		if (stageIndex >= stages.size())
			return CompletableFuture.completedFuture(resolveCompletion(completionResult));

		StepStage stage = stages.get(stageIndex);
		if (stage == null)
			return executeStages(context, flow, stages, stageIndex + 1, completionResult);

		if (!stage.supports(context, flow))
			return executeStages(context, flow, stages, stageIndex + 1, completionResult);

		CompletableFuture<StageOutcome> outcome = (stage.providerStage()
				? providerStageRunner.run(stage, context, flow, completionResult)
				: globalStageRunner.run(stage, context, flow, completionResult))
				.toCompletableFuture();

		return outcome.thenCompose(stageOutcome -> applyOutcome(stageOutcome, flow, stages, stageIndex));
	}

	private CompletableFuture<StepResult> applyOutcome(
			@Nullable StageOutcome outcome,
			@NotNull AuthFlowType flow,
			@NotNull List<StepStage> stages,
			int stageIndex
	) {
		if (outcome == null) {
			return CompletableFuture.completedFuture(StepResult.failed(
					joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed())
			));
		}

		storeContext(outcome.getContext());

		PendingStage pendingStage = outcome.getPendingStage();
		if (pendingStage != null) {
			storeWaiting(outcome.getContext(), flow, stages, stageIndex, pendingStage, outcome.getCompletionResult());
			return CompletableFuture.completedFuture(outcome.getResult());
		}

		if (outcome.isAdvanceStage())
			return executeStages(outcome.getContext(), flow, stages, stageIndex + 1, outcome.getCompletionResult());

		return CompletableFuture.completedFuture(outcome.getResult());
	}

	private AuthFlowType resolveFlow(@NotNull AuthContext context) {
		AuthFlowType preferred = resolvePreferredFlow();
		List<InternalProvider> preferredProviders = eligibilityService.eligibleProviders(context, preferred);
		if (!preferredProviders.isEmpty())
			return preferred;

		AuthFlowType fallback = preferred == AuthFlowType.SEAMLESS
				? AuthFlowType.INTERACTIVE
				: AuthFlowType.SEAMLESS;

		List<InternalProvider> fallbackProviders = eligibilityService.eligibleProviders(context, fallback);
		if (!fallbackProviders.isEmpty())
			return fallback;

		return null;
	}

	private AuthFlowType resolvePreferredFlow() {
		Settings settings = settingsProvider.get();
		Settings.Authentication authentication = settings != null ? settings.getAuthentication() : null;
		AuthFlowType flow = authentication != null ? authentication.getFlow() : null;
		return flow != null ? flow : AuthFlowType.SEAMLESS;
	}

	private StepResult finalizeFlow(
			@Nullable AuthContext context,
			@Nullable AuthFlowType flow,
			@Nullable StepResult result
	) {
		if (context == null || result == null || result.getStatus() == null) return result;
		if (result.getStatus() == StepResult.StepStatus.WAITING) return result;

		AuthContext updated = result.getUpdatedContext() != null ? result.getUpdatedContext() : context;
		if (flow != null)
			eventManager.call(new AuthFlowFinishedEvent(updated, flow, result));

		return result;
	}

	private void storeWaiting(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@NotNull List<StepStage> stages,
			int stageIndex,
			@NotNull PendingStage pendingStage,
			@Nullable StepResult completionResult
	) {
		if (context.getConnectionUniqueId() == null) return;
		FlowState flowState = new FlowState(flow, stages, stageIndex, pendingStage, context, completionResult);
		ConnectionState state = connectionStateRegistry.ensure(context.getConnectionUniqueId());
		state.putFlowState(flowState);
		state.putContext(context);
		connectionStateRegistry.storePending(state);
	}

	private StepResult resolveCompletion(@Nullable StepResult completionResult) {
		if (completionResult != null)
			return completionResult;

		return StepResult.failed(joinMessage(messagesProvider.get().getAuthentication().getNoCompletionStep()));
	}

	private String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

	private void storeContext(@Nullable AuthContext context) {
		if (context == null) return;
		UUID connectionId = context.getConnectionUniqueId();
		if (connectionId == null) return;
		connectionStateRegistry.ensure(connectionId).putContext(context);
	}

	private int findFirstStageIndex(
			@NotNull List<StepStage> stages,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	) {
		for (int index = 0; index < stages.size(); index++) {
			StepStage stage = stages.get(index);
			if (stage != null && stage.supports(context, flow))
				return index;
		}
		return -1;
	}
}
