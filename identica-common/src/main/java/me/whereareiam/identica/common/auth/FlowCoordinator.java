package me.whereareiam.identica.common.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.IdenticaKeys;
import me.whereareiam.identica.auth.attempt.AuthAttempt;
import me.whereareiam.identica.auth.attempt.AuthAttemptStore;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.common.auth.stage.StageOutcome;
import me.whereareiam.identica.common.auth.stage.runner.GlobalStageRunner;
import me.whereareiam.identica.common.auth.stage.runner.ProviderStageRunner;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.flow.AuthFlowFinishedEvent;
import me.whereareiam.identica.event.auth.flow.AuthFlowStartedEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.auth.stage.PendingStage;
import me.whereareiam.identica.auth.stage.StepStage;
import me.whereareiam.identica.auth.stage.StepStageRegistry;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
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
	private final AuthAttemptStore attemptStore;

	public CompletableFuture<StepResult> authenticate(@Nullable AuthContext context) {
		if (context == null) {
			return CompletableFuture.completedFuture(StepResult.failed(
					joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed())
			));
		}

		ResumeRequest pendingRequest = ResumeRequest.builder()
				.connectionUniqueId(context.getConnectionUniqueId())
				.identity(context.getIdentity())
				.build();
		if (attemptStore.hasPending(pendingRequest)) {
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
			@NotNull ResumeRequest request,
			@Nullable Consumer<AuthContext> contextUpdater
	) {
		AuthAttempt waiting = resolveWaiting(request);
		if (waiting == null)
			return CompletableFuture.completedFuture(StepResult.noPending());

		logResume(waiting, request.getConnectionUniqueId());

		AuthContext merged = mergeContext(waiting.getContext(), request);
		if (contextUpdater != null)
			contextUpdater.accept(merged);

		AuthFlowType flow = waiting.getFlow();
		merged.put(IdenticaKeys.CURRENT_FLOW, flow);

		AuthAttempt resumed = rebuildAttempt(waiting, merged);
		CompletableFuture<StepResult> result = resumeFromAttempt(resumed);
		return result.thenApply(stepResult -> finalizeFlow(merged, flow, attachContext(stepResult, merged)));
	}

	public boolean hasPending(@Nullable UUID connectionUniqueId) {
		if (connectionUniqueId == null) return false;
		ResumeRequest request = ResumeRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();

		return attemptStore.hasPending(request);
	}

	public boolean clearPending(@Nullable UUID connectionUniqueId) {
		if (connectionUniqueId == null)
			return false;

		ResumeRequest request = ResumeRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();
		boolean removed = attemptStore.consume(request).isPresent();
		eventManager.call(new me.whereareiam.identica.event.auth.AuthPendingClearedEvent(connectionUniqueId, removed));

		return removed;
	}

private CompletableFuture<StepResult> resumeFromAttempt(@NotNull AuthAttempt waiting) {
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
		if (!preferredProviders.isEmpty()) {
			Logger.debug("Auth flow selected: %s", preferred);
			return preferred;
		}

		AuthFlowType fallback = preferred == AuthFlowType.SEAMLESS
				? AuthFlowType.INTERACTIVE
				: AuthFlowType.SEAMLESS;

		List<InternalProvider> fallbackProviders = eligibilityService.eligibleProviders(context, fallback);
		if (!fallbackProviders.isEmpty()) {
			Logger.debug("Auth flow fallback selected: %s", fallback);
			return fallback;
		}

		Logger.debug("No eligible providers for auth flow");
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
		AuthAttempt attempt = new AuthAttempt(
				UUID.randomUUID(),
				flow,
				stages,
				stageIndex,
				pendingStage,
				context,
				completionResult,
				System.currentTimeMillis()
		);
		attemptStore.store(attempt, resolvePendingTtlMillis());
		logPendingStored(attempt);
	}

	private StepResult resolveCompletion(@Nullable StepResult completionResult) {
		if (completionResult != null)
			return completionResult;

		return StepResult.failed(joinMessage(messagesProvider.get().getAuthentication().getNoCompletionStep()));
	}

	private StepResult attachContext(
			@Nullable StepResult result,
			@Nullable AuthContext context
	) {
		if (result == null || context == null) return result;
		if (result.getUpdatedContext() != null) return result;
		if (result.getStatus() == null) return result;

		return new StepResult(result.getStatus(), result.getMessage(), context, result.getHandshakeMode());
	}

	private String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

	private long resolvePendingTtlMillis() {
		Settings.Authentication authentication = settingsProvider.get().getAuthentication();
		java.time.Duration configured = authentication.getPendingTtl();
		if (configured.isZero() || configured.isNegative())
			throw new IllegalStateException("settings.authentication.pendingTtl must be positive");

		return configured.toMillis();
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

	private @Nullable AuthAttempt resolveWaiting(@NotNull ResumeRequest request) {
		return attemptStore.consume(request).orElse(null);
	}

	private @NotNull AuthAttempt rebuildAttempt(@NotNull AuthAttempt waiting, @NotNull AuthContext context) {
		return new AuthAttempt(
				waiting.getAttemptId(),
				waiting.getFlow(),
				waiting.getStages(),
				waiting.getStageIndex(),
				waiting.getPendingStage(),
				context,
				waiting.getCompletionResult(),
				waiting.getCreatedAt()
		);
	}

	private @NotNull AuthContext mergeContext(@NotNull AuthContext base, @NotNull ResumeRequest request) {
		UUID connectionId = request.getConnectionUniqueId() != null
				? request.getConnectionUniqueId()
				: base.getConnectionUniqueId();

		String username = request.getUsername() != null ? request.getUsername() : base.getUsername();
		if (username == null || username.isBlank())
			return base;

		String ip = request.getIp() != null ? request.getIp() : base.getIp();
		ConnectionIdentity identity = new ConnectionIdentity(connectionId, username, ip);

		String intendedServer = request.getIntendedServer() != null
				? request.getIntendedServer()
				: base.getIntendedServer();

		return AuthContext.builder()
				.connectionUniqueId(connectionId)
				.identity(identity)
				.intendedServer(intendedServer)
				.provider(base.getProvider())
				.data(new HashMap<>(base.getData()))
				.build();
	}

	private void logPendingStored(@NotNull AuthAttempt attempt) {
		UUID connectionId = attempt.getContext().getConnectionUniqueId();
		String stageId = resolveStageId(attempt);
		String stepName = resolvePendingStepName(attempt.getPendingStage());
		Logger.debug("Stored pending auth state (connection: %s, flow: %s, stage: %s, step: %s)",
				connectionId,
				attempt.getFlow(),
				stageId,
				stepName != null ? stepName : "unknown");
	}

	private void logResume(@NotNull AuthAttempt attempt, @Nullable UUID connectionId) {
		String stageId = resolveStageId(attempt);
		Logger.debug("Resuming auth flow %s (connection: %s, stage: %s)",
				attempt.getFlow(),
				connectionId,
				stageId);
	}

	private @NotNull String resolveStageId(@NotNull AuthAttempt attempt) {
		int stageIndex = attempt.getStageIndex();
		List<StepStage> stages = attempt.getStages();
		if (stageIndex >= 0 && stageIndex < stages.size()) {
			StepStage stage = stages.get(stageIndex);
			if (stage != null && !stage.id().isBlank())
				return stage.id();
		}
		return "unknown";
	}

	private @Nullable String resolvePendingStepName(@NotNull PendingStage pendingStage) {
		int stepIndex = pendingStage.getStepIndex();
		List<AuthenticationStep> steps = pendingStage.getSteps();
		if (stepIndex < 0 || stepIndex >= steps.size())
			return null;

		AuthenticationStep step = steps.get(stepIndex);
		return step != null ? step.getName() : null;
	}
}
