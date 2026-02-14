package me.whereareiam.identica.engine.pipeline.scenario.shared;

import com.google.inject.Provider;
import me.whereareiam.identica.event.auth.AuthContextBuildEvent;
import me.whereareiam.identica.event.pipeline.attempt.ScenarioContextBuiltEvent;
import me.whereareiam.identica.event.registration.RegistrationContextBuildEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.PipelineCursor;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.group.GroupOutcome;
import me.whereareiam.identica.pipeline.group.PipelineGroup;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.util.EventUtil;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractScenarioPipeline {
	private final PipelineRegistry registry;
	private final Provider<Messages> messagesProvider;
	private final Provider<Settings> settingsProvider;
	private final PipelineStateStore pipelineStateStore;
	private final PipelineType pipelineType;

	protected AbstractScenarioPipeline(
			@NotNull PipelineRegistry registry,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull Provider<Settings> settingsProvider,
			@NotNull PipelineStateStore pipelineStateStore,
			@NotNull PipelineType pipelineType
	) {
		this.registry = registry;
		this.messagesProvider = messagesProvider;
		this.settingsProvider = settingsProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.pipelineType = pipelineType;
	}

	public CompletionStage<PipelineResult> execute(@Nullable ConnectionRequest request) {
		return execute(request, null);
	}

	public CompletionStage<PipelineResult> execute(
			@Nullable ConnectionRequest request,
			@Nullable ResumeRequest resumeRequest
	) {
		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(pipelineType);

		ResumeResolution resumeResolution = resolveResume(request, resumeRequest);
		if (resumeResolution.result() != null)
			return CompletableFuture.completedFuture(resumeResolution.result());
		if (resumeResolution.state() != null)
			pipelineState = resumeResolution.state();
		markScenarioStart(pipelineState, resumeResolution.state() != null);

		PipelineResult startResult = ensureScenario(pipelineState, request);
		if (startResult != null)
			return CompletableFuture.completedFuture(startResult);

		return run(pipelineState, resumeRequest);
	}

	private @NotNull CompletableFuture<PipelineResult> run(
			@NotNull PipelineState pipelineState,
			@Nullable ResumeRequest resumeRequest
	) {
		ExecutionSnapshot snapshot = snapshot(pipelineState);
		if (snapshot.groups().isEmpty())
			return CompletableFuture.completedFuture(failedAuth(pipelineType));

		ExecutionState executionState = new ExecutionState();
		return executeGroups(snapshot, pipelineState, 0, executionState)
				.thenApply(ignored -> {
					PipelineResult result = executionState.result;
					if (result == null) result = failedNoCompletion(pipelineType);
					result = result.withState(pipelineState);
					persistState(pipelineState, result, resumeRequest);

					return result;
				})
				.toCompletableFuture();
	}

	private @NotNull ExecutionSnapshot snapshot(@NotNull PipelineState pipelineState) {
		List<PipelineGroup<?>> groups = registry.resolve(pipelineState);
		List<GroupSnapshot<?>> snapshots = new ArrayList<>();
		for (PipelineGroup<?> group : groups) {
			if (group == null) continue;
			snapshots.add(snapshotGroup(group));
		}

		return new ExecutionSnapshot(List.copyOf(snapshots));
	}

	private <S> @NotNull GroupSnapshot<S> snapshotGroup(@NotNull PipelineGroup<?> untypedGroup) {
		@SuppressWarnings("unchecked")
		PipelineGroup<S> group = (PipelineGroup<S>) untypedGroup;
		List<PipelinePhase<S>> phases = registry.resolvePhases(group.id(), group.stateType());

		return new GroupSnapshot<>(group, List.copyOf(phases));
	}

	private @NotNull CompletionStage<Void> executeGroups(
			@NotNull ExecutionSnapshot snapshot,
			@NotNull PipelineState pipelineState,
			int groupIndex,
			@NotNull ExecutionState executionState
	) {
		if (executionState.stopped || groupIndex < 0 || groupIndex >= snapshot.groups().size())
			return CompletableFuture.completedFuture(null);

		GroupSnapshot<?> groupSnapshot = snapshot.groups().get(groupIndex);
		if (groupSnapshot == null) {
			return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState);
		}

		return executeTypedGroup(snapshot, pipelineState, groupIndex, groupSnapshot, executionState);
	}

	private <S> @NotNull CompletionStage<Void> executeTypedGroup(
			@NotNull ExecutionSnapshot snapshot,
			@NotNull PipelineState pipelineState,
			int groupIndex,
			@NotNull GroupSnapshot<?> untypedGroup,
			@NotNull ExecutionState executionState
	) {
		@SuppressWarnings("unchecked")
		GroupSnapshot<S> groupSnapshot = (GroupSnapshot<S>) untypedGroup;
		PipelineGroup<S> group = groupSnapshot.group();
		if (!group.supports(pipelineState)) {
			return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState);
		}

		S initialState = group.initializeState(pipelineState, executionState.result);
		List<PipelinePhase<S>> phases = groupSnapshot.phases();

		return executePhases(pipelineState, group.id(), phases, 0, initialState)
				.thenCompose(state -> {
					GroupOutcome outcome = group.complete(pipelineState, state);
					if (outcome.getResult() != null)
						executionState.result = outcome.getResult();
					if (outcome.isStopped())
						executionState.stopped = true;
					return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState);
				});
	}

	private <S> @NotNull CompletionStage<S> executePhases(
			@NotNull PipelineState pipelineState,
			@NotNull String groupId,
			@NotNull List<PipelinePhase<S>> phases,
			int phaseIndex,
			@NotNull S state
	) {
		if (phaseIndex >= phases.size()) {
			return CompletableFuture.completedFuture(state);
		}

		PipelinePhase<S> phase = phases.get(phaseIndex);
		if (phase == null || !phase.supports(pipelineState, state)) {
			return executePhases(pipelineState, groupId, phases, phaseIndex + 1, state);
		}

		pipelineState.setCursor(new PipelineCursor(groupId, phase.id()));
		return phase.execute(pipelineState, state).toCompletableFuture()
				.thenCompose(result -> {
					PhaseResult<S> resolved = result != null ? result : PhaseResult.pass(state);
					return executePhases(pipelineState, groupId, phases, phaseIndex + 1, resolved.getState());
				});
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
			Settings.Scenario scenario = resolveScenario(settingsProvider.get(), pipelineType);
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

	private @NotNull PipelineResult failedAuth(@NotNull PipelineType type) {
		return PipelineResult.failed(joinMessage(resolveFailureMessage(type)));
	}

	private @NotNull PipelineResult failedNoCompletion(@NotNull PipelineType type) {
		return PipelineResult.failed(joinMessage(resolveScenarioMessages(type).getNoCompletionPipeline()));
	}

	@NotNull
	public ConnectionDecision mapDecision(
			@Nullable PipelineResult result
	) {
		if (result == null) return failureDecision();

		return switch (result.getStatus()) {
			case CONTINUE, COMPLETE -> ConnectionDecision.allow();
			case WAITING -> ConnectionDecision.waiting(result.getMessage());
			case FAILED, DENIED -> ConnectionDecision.deny(messageOrFallback(result.getMessage()));
			case REQUIRE_RECONNECT -> ConnectionDecision.requireReconnect(messageOrFallback(result.getMessage()));
			case NO_PENDING -> ConnectionDecision.noPending();
		};
	}

	@NotNull
	public ConnectionDecision failureDecision() {
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

	protected @Nullable ScenarioContext resolveScenarioContext(@NotNull PipelineResult result) {
		PipelineState state = result.getState();
		if (state == null)
			return null;

		ScenarioContext scenario = state.getScenario(pipelineType);
		if (scenario == null)
			scenario = state.getScenario();

		return scenario;
	}

	private @NotNull Messages.Connection.Scenario resolveScenarioMessages(@NotNull PipelineType type) {
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (type == PipelineType.REGISTRATION)
			return connection.getRegistration();
		if (type == PipelineType.MIGRATION)
			return connection.getMigration();
		return connection.getAuthentication();
	}

	private @NotNull List<String> resolveFailureMessage(@NotNull PipelineType type) {
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (type == PipelineType.REGISTRATION) {
			return connection.getRegistration().getRegistrationFailed();
		}
		if (type == PipelineType.MIGRATION) {
			return connection.getMigration().getMigrationFailed();
		}

		return connection.getAuthentication().getAuthenticationFailed();
	}

	private @NotNull Settings.Scenario resolveScenario(
			@NotNull Settings settings,
			@NotNull PipelineType type
	) {
		Settings.Connection connection = settings.getConnection();
		if (type == PipelineType.REGISTRATION)
			return connection.getRegistration();
		if (type == PipelineType.MIGRATION)
			return connection.getMigration();
		return connection.getAuthentication();
	}

	private @Nullable PipelineResult ensureScenario(
			@NotNull PipelineState pipelineState,
			@Nullable ConnectionRequest request
	) {
		if (pipelineState.getScenario() != null) return null;

		if (request == null) {
			Logger.severe("%s request missing identity (connection request unresolved)", pipelineType);
			return failedAuth(pipelineType);
		}

		ScenarioContext context = buildContext(request);
		if (context == null) {
			return failedAuth(pipelineType);
		}

		pipelineState.setScenario(context);
		return null;
	}

	private @Nullable ScenarioContext buildContext(@NotNull ConnectionRequest request) {
		if (request.getIdentity().getUniqueId() == null) {
			UUID fallbackUniqueId = request.getConnectionUniqueId();
			if (fallbackUniqueId == null)
				fallbackUniqueId = UniqueIdGenerator.offlinePlayerUniqueId(request.getUsername());

			if (fallbackUniqueId == null) {
				Logger.severe("%s request missing Identica UUID and fallback UUID", pipelineType);
				return null;
			}

			Logger.warn("%s request missing Identica UUID, applying fallback UUID %s", pipelineType, fallbackUniqueId);
			request.getIdentity().setUniqueId(fallbackUniqueId);
		}

		ScenarioContext context = switch (pipelineType) {
			case REGISTRATION -> RegistrationContext.builder()
					.connectionUniqueId(request.getConnectionUniqueId())
					.identity(request.getIdentity())
					.intendedServer(request.getIntendedServer())
					.build();
			case MIGRATION -> me.whereareiam.identica.model.migration.MigrationContext.builder()
					.connectionUniqueId(request.getConnectionUniqueId())
					.identity(request.getIdentity())
					.intendedServer(request.getIntendedServer())
					.accountUniqueId(request.getIdentity().getUniqueId())
					.build();
			case AUTHENTICATION -> AuthContext.builder()
					.connectionUniqueId(request.getConnectionUniqueId())
					.identity(request.getIdentity())
					.intendedServer(request.getIntendedServer())
					.build();
		};

		if (context instanceof AuthContext authContext)
			EventUtil.callEvent(new AuthContextBuildEvent(authContext));
		if (context instanceof RegistrationContext registrationContext)
			EventUtil.callEvent(new RegistrationContextBuildEvent(registrationContext));

		EventUtil.callEvent(new ScenarioContextBuiltEvent(context));

		return context;
	}

	private @NotNull ResumeResolution resolveResume(
			@Nullable ConnectionRequest request,
			@Nullable ResumeRequest resumeRequest
	) {
		if (resumeRequest == null) return new ResumeResolution(null, null);

		Settings.Scenario scenario = resolveScenario(settingsProvider.get(), pipelineType);
		if (!scenario.isAllowResume()) {
			pipelineStateStore.clear(PipelineStateReference.from(resumeRequest));
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

		JourneyStateItem pending = stored.item(JourneyStateItem.class).orElse(null);
		if (pending == null) {
			return resumeUnavailable(request);
		}

		ScenarioContext merged = mergeContext(storedContext, resumeRequest);
		stored.setScenario(merged);
		stored.setPipelineType(pipelineType);

		return new ResumeResolution(stored, null);
	}

	private @NotNull ResumeResolution resumeUnavailable(@Nullable ConnectionRequest request) {
		return request == null
				? new ResumeResolution(null, PipelineResult.noPending())
				: new ResumeResolution(null, null);
	}

	private void markScenarioStart(@NotNull PipelineState pipelineState, boolean resumed) {
		if (pipelineType == PipelineType.AUTHENTICATION) {
			me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem identity =
					pipelineState.item(me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem.class)
							.orElse(null);
			if (identity == null) {
				identity = new me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem();
			}
			identity.setResumed(resumed);
			pipelineState.putItem(identity, 0L);
			return;
		}

		if (pipelineType == PipelineType.REGISTRATION) {
			me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityMetaItem identity =
					pipelineState.item(me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityMetaItem.class)
							.orElse(null);
			if (identity == null) {
				identity = new me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityMetaItem();
			}
			identity.setResumed(resumed);
			pipelineState.putItem(identity, 0L);
			return;
		}

		if (pipelineType == PipelineType.MIGRATION) {
			me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem identity =
					pipelineState.item(me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem.class)
							.orElse(null);
			if (identity == null) {
				identity = new me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem();
			}
			identity.setResumed(resumed);
			pipelineState.putItem(identity, 0L);
		}
	}

	private @NotNull ScenarioContext mergeContext(
			@NotNull ScenarioContext base,
			@NotNull ResumeRequest request
	) {
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

		switch (base) {
			case RegistrationContext registration -> {
				RegistrationContext merged = RegistrationContext.builder()
						.connectionUniqueId(connectionId)
						.identity(identity)
						.intendedServer(intendedServer)
						.build();
				merged.setProvider(registration.getProvider());
				return merged;
			}
			case MigrationContext migration -> {
				MigrationContext merged = MigrationContext.builder()
						.connectionUniqueId(connectionId)
						.identity(identity)
						.intendedServer(intendedServer)
						.targetProviderId(migration.getTargetProviderId())
						.accountUniqueId(migration.getAccountUniqueId())
						.build();
				merged.setProvider(migration.getProvider());
				return merged;
			}
			case AuthContext authContext -> {
				AuthContext merged = AuthContext.builder()
						.connectionUniqueId(connectionId)
						.identity(identity)
						.intendedServer(intendedServer)
						.build();
				merged.setProvider(authContext.getProvider());
				return merged;
			}
			default -> {
			}
		}

		return base;
	}

	private String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

	private record GroupSnapshot<S>(
			@NotNull PipelineGroup<S> group,
			@NotNull List<PipelinePhase<S>> phases
	) {
	}

	private record ExecutionSnapshot(
			@NotNull List<GroupSnapshot<?>> groups
	) {
	}

	private static final class ExecutionState {
		private @Nullable PipelineResult result;
		private boolean stopped;
	}

	private record ResumeResolution(
			@Nullable PipelineState state,
			@Nullable PipelineResult result
	) {
	}
}
