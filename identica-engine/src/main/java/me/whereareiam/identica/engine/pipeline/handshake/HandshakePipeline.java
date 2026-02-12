package me.whereareiam.identica.engine.pipeline.handshake;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.handshake.state.HandshakeRequestState;
import me.whereareiam.identica.engine.pipeline.handshake.state.HandshakeState;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.pipeline.PipelineCursor;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.group.GroupOutcome;
import me.whereareiam.identica.pipeline.group.PipelineGroup;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class HandshakePipeline {
	private final HandshakePipelineRegistry registry;

	public @NotNull CompletionStage<HandshakeDecision> execute(@Nullable HandshakeRequest request) {
		if (request == null)
			return CompletableFuture.completedFuture(HandshakeDecision.allow());

		PipelineState pipelineState = PipelineState.initial();
		HandshakeRequestState requestState = HandshakeRequestState.from(request);
		pipelineState.putItem(requestState, 0L);

		ExecutionSnapshot snapshot = snapshot(pipelineState);
		if (snapshot.groups().isEmpty())
			return CompletableFuture.completedFuture(HandshakeDecision.allow());

		ExecutionState executionState = new ExecutionState();
		return executeGroups(snapshot, pipelineState, 0, executionState)
				.thenApply(ignored -> executionState.decision != null
						? executionState.decision
						: HandshakeDecision.allow())
				.toCompletableFuture();
	}

	private @NotNull ExecutionSnapshot snapshot(@NotNull PipelineState pipelineState) {
		List<PipelineGroup<?>> groups = registry.resolve(pipelineState);
		List<GroupSnapshot<?>> snapshots = new ArrayList<>();
		for (PipelineGroup<?> group : groups) {
			if (group == null)
				continue;
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
		if (groupSnapshot == null)
			return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState);

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
		if (!group.supports(pipelineState))
			return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState);

		S initialState = group.initializeState(pipelineState, null);
		List<PipelinePhase<S>> phases = groupSnapshot.phases();

		return executePhases(pipelineState, group.id(), phases, 0, initialState)
				.thenCompose(state -> {
					GroupOutcome outcome = group.complete(pipelineState, state);
					if (outcome.isStopped())
						executionState.stopped = true;

					if (state instanceof HandshakeState handshakeState)
						executionState.decision = handshakeState.resolvedDecision();

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
		if (phaseIndex >= phases.size())
			return CompletableFuture.completedFuture(state);

		PipelinePhase<S> phase = phases.get(phaseIndex);
		if (phase == null)
			return executePhases(pipelineState, groupId, phases, phaseIndex + 1, state);
		if (!phase.supports(pipelineState, state))
			return executePhases(pipelineState, groupId, phases, phaseIndex + 1, state);

		pipelineState.setCursor(new PipelineCursor(groupId, phase.id()));

		return phase.execute(pipelineState, state).toCompletableFuture()
				.thenCompose(result -> {
					PhaseResult<S> resolved = result != null ? result : PhaseResult.pass(state);
					return executePhases(pipelineState, groupId, phases, phaseIndex + 1, resolved.getState());
				});
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
		private @Nullable HandshakeDecision decision;
		private boolean stopped;
	}
}
