package me.whereareiam.identica.engine.pipeline.scenario;

import com.google.inject.Provider;
import me.whereareiam.identica.engine.pipeline.PipelineExecutor;
import me.whereareiam.identica.event.pipeline.attempt.ScenarioContextBuiltEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.AdvanceMarkerItem;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.group.GroupOutcome;
import me.whereareiam.identica.pipeline.group.PipelineGroup;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractScenarioPipeline {
	private static final long ADVANCE_LOCK_FALLBACK_MS = 5_000L;

	private final PipelineRegistry registry;
	private final Provider<Messages> messagesProvider;
	private final Provider<Settings> settingsProvider;
	private final PipelineStateStore pipelineStateStore;
	private final PipelineType pipelineType;
	private final PipelineExecutor executor;

	protected AbstractScenarioPipeline(
			@NotNull PipelineRegistry registry,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull Provider<Settings> settingsProvider,
			@NotNull PipelineStateStore pipelineStateStore,
			@NotNull PipelineExecutor executor,
			@NotNull PipelineType pipelineType
	) {
		this.registry = registry;
		this.messagesProvider = messagesProvider;
		this.settingsProvider = settingsProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.executor = executor;
		this.pipelineType = pipelineType;
	}

	public @NotNull CompletionStage<PipelineResult> execute(@Nullable ConnectionRequest request) {
		return execute(request, null, PendingMode.RESUME);
	}

	public @NotNull CompletionStage<PipelineResult> execute(
			@Nullable ConnectionRequest request,
			@Nullable ResumeRequest resumeRequest
	) {
		return execute(request, resumeRequest, PendingMode.RESUME);
	}

	public @NotNull CompletionStage<PipelineResult> executeAdvance(@NotNull ResumeRequest advanceRequest) {
		return execute(null, advanceRequest, PendingMode.ADVANCE);
	}

	private @NotNull CompletionStage<PipelineResult> execute(
			@Nullable ConnectionRequest request,
			@Nullable ResumeRequest pendingRequest,
			@NotNull PendingMode pendingMode
	) {
		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(pipelineType);

		ResumeResolution pendingResolution = resolvePending(request, pendingRequest, pendingMode);
		if (pendingResolution.result() != null)
			return CompletableFuture.completedFuture(pendingResolution.result());
		if (pendingResolution.state() != null)
			pipelineState = pendingResolution.state();

		onStart(pipelineState, pendingResolution.state() != null);

		PipelineResult startResult = ensureScenario(pipelineState, request);
		if (startResult != null)
			return CompletableFuture.completedFuture(startResult);

		return run(pipelineState, pendingRequest, pendingMode);
	}

	public @NotNull PipelineType type() {
		return pipelineType;
	}

	public @NotNull PipelineRegistry registry() {
		return registry;
	}

	public @Nullable ScenarioContext resolveContext(@NotNull PipelineResult result) {
		return resolveScenarioContext(result);
	}

	public boolean matchesNewFlow(@Nullable ConnectionRequest request) {
		return false;
	}

	public @NotNull ConnectionDecision mapDecision(@Nullable PipelineResult result) {
		if (result == null) return failureDecision();

		return switch (result.getStatus()) {
			case CONTINUE, COMPLETE -> ConnectionDecision.allow();
			case WAITING -> ConnectionDecision.waiting(result.getMessage());
			case FAILED, DENIED -> ConnectionDecision.deny(messageOrFallback(result.getMessage()));
			case REQUIRE_RECONNECT -> ConnectionDecision.requireReconnect(messageOrFallback(result.getMessage()));
			case NO_PENDING -> ConnectionDecision.noPending();
		};
	}

	public @NotNull ConnectionDecision failureDecision() {
		return ConnectionDecision.deny(failureMessage());
	}

	protected @NotNull String messageOrFallback(@Nullable String message) {
		if (message != null && !message.isBlank())
			return message;
		return failureMessage();
	}

	protected @NotNull String failureMessage() {
		return joinMessage(resolveFailureMessage(pipelineType));
	}

	protected @NotNull String noCompletionMessage() {
		return joinMessage(resolveScenarioMessages(pipelineType).getNoCompletionPipeline());
	}

	protected @Nullable ScenarioContext resolveScenarioContext(@NotNull PipelineResult result) {
		PipelineState state = result.getState();
		if (state == null)
			return null;

		ScenarioContext scenario = state.getScenario(pipelineType);
		if (scenario == null)
			scenario = state.getScenario();

		return scenario;
	}

	protected abstract @Nullable ScenarioContext buildContext(@NotNull ConnectionRequest request);

	protected abstract @NotNull ScenarioContext mergeContext(@NotNull ScenarioContext base, @NotNull ResumeRequest request);

	protected abstract boolean isPending(@NotNull PipelineState state);

	protected abstract void onStart(@NotNull PipelineState state, boolean resumed);

	private @NotNull CompletableFuture<PipelineResult> run(
			@NotNull PipelineState pipelineState,
			@Nullable ResumeRequest pendingRequest,
			@NotNull PendingMode pendingMode
	) {
		ExecutionState executionState = new ExecutionState();
		return executor.execute(registry, pipelineState, new PipelineExecutor.ExecutionObserver() {
					@Override
					public @NotNull <S> S initializeState(
							@NotNull PipelineGroup<S> group,
							@NotNull PipelineState pipelineState,
							PipelineResult currentResult
					) {
						return group.initializeState(pipelineState, currentResult);
					}

					@Override
					public <S> void onGroupCompleted(
							@NotNull PipelineGroup<S> group,
							@NotNull S state,
							@NotNull GroupOutcome outcome,
							PipelineResult currentResult
					) {
						if (outcome.getResult() == null) return;
						executionState.result = outcome.getResult();
					}
				})
				.thenApply(ignored -> {
					PipelineResult result = executionState.result;
					if (result == null) result = failedNoCompletion();
					result = result.withState(pipelineState);
					if (pendingMode == PendingMode.ADVANCE) {
						pipelineState.removeItem(AdvanceMarkerItem.class);
					}
					persistState(pipelineState, result, pendingRequest);

					return result;
				})
				.toCompletableFuture();
	}

	private void persistState(
			@NotNull PipelineState pipelineState,
			@NotNull PipelineResult result,
			@Nullable ResumeRequest resumeRequest
	) {
		PipelineStateReference reference = resolveReference(pipelineState, result, resumeRequest);
		if (reference == null || reference.isEmpty()) return;

		PipelineStatus status = result.getStatus();
		if (status == PipelineStatus.WAITING || status == PipelineStatus.REQUIRE_RECONNECT) {
			Settings.Scenario scenario = resolveScenario(pipelineType);
			if (!scenario.isAllowResume()) {
				pipelineStateStore.clear(reference);
				return;
			}
			long ttlMs = scenario.pipelineTtlMillis();
			if (ttlMs <= 0) {
				pipelineStateStore.clear(reference);
				return;
			}
			pipelineStateStore.save(reference, pipelineState, ttlMs);
			return;
		}

		pipelineStateStore.clear(reference);
	}

	private @Nullable PipelineStateReference resolveReference(
			@NotNull PipelineState pipelineState,
			@NotNull PipelineResult result,
			@Nullable ResumeRequest resumeRequest
	) {
		ScenarioContext scenarioContext = null;
		PipelineState resultState = result.getState();
		if (resultState != null)
			scenarioContext = resultState.getScenario(pipelineType);
		if (scenarioContext == null)
			scenarioContext = resolveScenarioContext(pipelineState);
		if (scenarioContext != null)
			return PipelineStateReference.from(scenarioContext);

		if (resumeRequest != null)
			return PipelineStateReference.from(resumeRequest);

		return null;
	}

	private @Nullable ScenarioContext resolveScenarioContext(@NotNull PipelineState pipelineState) {
		PipelineType stateType = pipelineState.getPipelineType();
		if (stateType == null) return null;

		return pipelineState.getScenario(stateType);
	}

	private @NotNull PipelineResult failedAuth() {
		return PipelineResult.failed(failureMessage());
	}

	private @NotNull PipelineResult failedNoCompletion() {
		return PipelineResult.failed(noCompletionMessage());
	}

	private @Nullable PipelineResult ensureScenario(
			@NotNull PipelineState pipelineState,
			@Nullable ConnectionRequest request
	) {
		if (pipelineState.getScenario() != null) return null;

		if (request == null) {
			Logger.severe("%s request missing identity (connection request unresolved)", pipelineType);
			return failedAuth();
		}

		ScenarioContext context = buildContext(request);
		if (context == null) {
			return failedAuth();
		}

		pipelineState.setScenario(context);
		return null;
	}

	private @NotNull ResumeResolution resolvePending(
			@Nullable ConnectionRequest request,
			@Nullable ResumeRequest pendingRequest,
			@NotNull PendingMode pendingMode
	) {
		if (pendingRequest == null) return new ResumeResolution(null, null);

		if (pendingMode == PendingMode.RESUME)
			return resolveResume(request, pendingRequest);

		return resolveAdvance(request, pendingRequest);
	}

	private @NotNull ResumeResolution resolveResume(
			@Nullable ConnectionRequest request,
			@NotNull ResumeRequest resumeRequest
	) {
		PipelineStateReference resumeReference = PipelineStateReference.from(resumeRequest);
		Settings.Scenario scenario = resolveScenario(pipelineType);
		if (!scenario.isAllowResume()) {
			pipelineStateStore.clear(resumeReference);
			return resumeUnavailable(request);
		}

		PipelineState stored = pipelineStateStore.consume(resumeRequest).orElse(null);
		if (stored == null) {
			return resumeUnavailable(request);
		}

		PipelineType storedType = stored.getPipelineType();
		ScenarioContext storedContext = stored.getScenario(pipelineType);
		if (storedType == null || storedType != pipelineType || storedContext == null) {
			return resumeUnavailable(request);
		}

		if (!isPending(stored)) {
			return resumeUnavailable(request);
		}

		ScenarioContext merged = mergeContext(storedContext, resumeRequest);
		stored.setScenario(merged);
		stored.setPipelineType(pipelineType);

		return new ResumeResolution(stored, null);
	}

	private @NotNull ResumeResolution resolveAdvance(
			@Nullable ConnectionRequest request,
			@NotNull ResumeRequest advanceRequest
	) {
		PipelineStateReference reference = PipelineStateReference.from(advanceRequest);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) {
			return resumeUnavailable(request);
		}

		PipelineType storedType = stored.getPipelineType();
		ScenarioContext storedContext = stored.getScenario(pipelineType);
		if (storedType == null || storedType != pipelineType || storedContext == null) {
			return resumeUnavailable(request);
		}

		if (!isPending(stored)) {
			return resumeUnavailable(request);
		}

		long now = System.currentTimeMillis();
		AdvanceMarkerItem lockItem = stored.item(AdvanceMarkerItem.class).orElse(null);
		if (lockItem != null && lockItem.getExpiresAt() > now) {
			return new ResumeResolution(null, PipelineResult.waiting(resolveScenarioMessages(pipelineType).getAdvanceBusy().getChat()));
		}

		long lockTtlMs = resolveAdvanceLockTtlMillis();
		AdvanceMarkerItem nextLock = new AdvanceMarkerItem(resolveLockOwner(advanceRequest), now + lockTtlMs);
		stored.putItem(nextLock, lockTtlMs);

		ScenarioContext merged = mergeContext(storedContext, advanceRequest);
		stored.setScenario(merged);
		stored.setPipelineType(pipelineType);

		long ttlMs = resolveScenario(pipelineType).pipelineTtlMillis();
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}

		return new ResumeResolution(stored, null);
	}

	private @Nullable UUID resolveLockOwner(@NotNull ResumeRequest request) {
		UUID ownerId = request.getConnectionUniqueId();
		if (ownerId != null) return ownerId;
		return request.getIdentityUniqueId();
	}

	private long resolveAdvanceLockTtlMillis() {
		try {
			long ttlMs = resolveScenario(pipelineType).advanceLockTtlMillis();
			return ttlMs > 0 ? ttlMs : ADVANCE_LOCK_FALLBACK_MS;
		} catch (Exception ignored) {
			return ADVANCE_LOCK_FALLBACK_MS;
		}
	}

	private @NotNull ResumeResolution resumeUnavailable(@Nullable ConnectionRequest request) {
		return request == null
				? new ResumeResolution(null, PipelineResult.noPending())
				: new ResumeResolution(null, null);
	}

	protected @NotNull Messages.Connection.Scenario resolveScenarioMessages(@NotNull PipelineType type) {
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (type == PipelineType.REGISTRATION)
			return connection.getRegistration();
		if (type == PipelineType.MIGRATION)
			return connection.getMigration();
		return connection.getAuthentication();
	}

	protected @NotNull List<String> resolveFailureMessage(@NotNull PipelineType type) {
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (type == PipelineType.REGISTRATION) {
			return connection.getRegistration().getRegistrationFailed();
		}
		if (type == PipelineType.MIGRATION) {
			return connection.getMigration().getMigrationFailed();
		}

		return connection.getAuthentication().getAuthenticationFailed();
	}

	protected @NotNull Settings.Scenario resolveScenario(@NotNull PipelineType type) {
		Settings.Connection connection = settingsProvider.get().getConnection();
		if (type == PipelineType.REGISTRATION)
			return connection.getRegistration();
		if (type == PipelineType.MIGRATION)
			return connection.getMigration();
		return connection.getAuthentication();
	}

	protected @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

	protected @Nullable ConnectionIdentity mergeIdentity(
			@Nullable ScenarioContext base,
			@NotNull ResumeRequest request
	) {
		if (base == null) return null;
		String username = request.getUsername() != null ? request.getUsername() : base.getUsername();
		if (username == null || username.isBlank())
			return null;

		String ip = request.getIp() != null ? request.getIp() : base.getIp();
		UUID identicaUniqueId = base.getIdenticaUniqueId();
		UUID observedUniqueId = request.getIdentityInfo() != null && request.getIdentityInfo().getObservedUniqueId() != null
				? request.getIdentityInfo().getObservedUniqueId()
				: base.getIdentity().getObservedUniqueId();
		ConnectionIdentity merged = new ConnectionIdentity(identicaUniqueId, observedUniqueId, username, ip);

		ConnectionIdentity requestIdentity = request.getIdentityInfo();
		if (requestIdentity != null) {
			merged.setOrigin(requestIdentity.getOrigin());
			return merged;
		}

		merged.setOrigin(base.getIdentity().getOrigin());

		return merged;
	}

	protected @Nullable UUID resolveConnectionUniqueId(
			@Nullable ScenarioContext base,
			@NotNull ResumeRequest request
	) {
		if (request.getConnectionUniqueId() != null)
			return request.getConnectionUniqueId();
		return base != null ? base.getConnectionUniqueId() : null;
	}

	protected void emitScenarioBuilt(@NotNull ScenarioContext context) {
		EventUtil.callEvent(new ScenarioContextBuiltEvent(context));
	}

	private enum PendingMode {
		RESUME,
		ADVANCE
	}

	private static final class ExecutionState {
		private @Nullable PipelineResult result;
	}

	private record ResumeResolution(
			@Nullable PipelineState state,
			@Nullable PipelineResult result
	) {
	}
}
