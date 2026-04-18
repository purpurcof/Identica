package me.whereareiam.identica.engine.pipeline;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.pipeline.PipelineCursor;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.model.pipeline.GroupOutcome;
import me.whereareiam.identica.pipeline.PipelineGroup;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class PipelineExecutor {
	public @NotNull CompletionStage<Void> execute(
			@NotNull PipelineRegistry registry,
			@NotNull PipelineState pipelineState,
			@NotNull ExecutionObserver observer
	) {
		ExecutionSnapshot snapshot = snapshot(registry, pipelineState);
		if (snapshot.groups().isEmpty())
			return CompletableFuture.completedFuture(null);

		ExecutionState executionState = new ExecutionState();
		return executeGroups(snapshot, pipelineState, 0, executionState, observer);
	}

	private @NotNull ExecutionSnapshot snapshot(
			@NotNull PipelineRegistry registry,
			@NotNull PipelineState pipelineState
	) {
		List<PipelineGroup<?>> groups = registry.resolve(pipelineState);
		List<GroupSnapshot<?>> snapshots = new ArrayList<>();
		for (PipelineGroup<?> group : groups) {
			if (group == null) continue;
			snapshots.add(snapshotGroup(registry, group));
		}
		return new ExecutionSnapshot(List.copyOf(snapshots));
	}

	private <S> @NotNull GroupSnapshot<S> snapshotGroup(
			@NotNull PipelineRegistry registry,
			@NotNull PipelineGroup<?> untypedGroup
	) {
		@SuppressWarnings("unchecked")
		PipelineGroup<S> group = (PipelineGroup<S>) untypedGroup;
		List<PipelinePhase<S>> phases = registry.resolvePhases(group.id(), group.stateType());

		return new GroupSnapshot<>(group, List.copyOf(phases));
	}

	private @NotNull CompletionStage<Void> executeGroups(
			@NotNull ExecutionSnapshot snapshot,
			@NotNull PipelineState pipelineState,
			int groupIndex,
			@NotNull ExecutionState executionState,
			@NotNull ExecutionObserver observer
	) {
		if (executionState.stopped || groupIndex < 0 || groupIndex >= snapshot.groups().size())
			return CompletableFuture.completedFuture(null);

		GroupSnapshot<?> groupSnapshot = snapshot.groups().get(groupIndex);
		if (groupSnapshot == null)
			return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState, observer);

		return executeTypedGroup(snapshot, pipelineState, groupIndex, groupSnapshot, executionState, observer);
	}

	private <S> @NotNull CompletionStage<Void> executeTypedGroup(
			@NotNull ExecutionSnapshot snapshot,
			@NotNull PipelineState pipelineState,
			int groupIndex,
			@NotNull GroupSnapshot<?> untypedGroup,
			@NotNull ExecutionState executionState,
			@NotNull ExecutionObserver observer
	) {
		@SuppressWarnings("unchecked")
		GroupSnapshot<S> groupSnapshot = (GroupSnapshot<S>) untypedGroup;
		PipelineGroup<S> group = groupSnapshot.group();
		if (!group.supports(pipelineState))
			return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState, observer);

		S initialState = observer.initializeState(group, pipelineState, executionState.result);
		List<PipelinePhase<S>> phases = groupSnapshot.phases();

		return executePhases(pipelineState, group.id(), phases, 0, initialState)
				.thenCompose(state -> {
					GroupOutcome outcome = group.complete(pipelineState, state);
					if (outcome.getResult() != null)
						executionState.result = outcome.getResult();
					if (outcome.isStopped())
						executionState.stopped = true;
					observer.onGroupCompleted(group, state, outcome, executionState.result);
					return executeGroups(snapshot, pipelineState, groupIndex + 1, executionState, observer);
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
		if (phase == null) return executePhases(pipelineState, groupId, phases, phaseIndex + 1, state);
		if (!phase.supports(pipelineState, state))
			return executePhases(pipelineState, groupId, phases, phaseIndex + 1, state);

		pipelineState.setCursor(new PipelineCursor(groupId, phase.id()));
		return phase.execute(pipelineState, state).toCompletableFuture()
				.thenCompose(result -> {
					PhaseResult<S> resolved = result != null ? result : PhaseResult.pass(state);
					return executePhases(pipelineState, groupId, phases, phaseIndex + 1, resolved.getState());
				});
	}

	public interface ExecutionObserver {
		@NotNull <S> S initializeState(
				@NotNull PipelineGroup<S> group,
				@NotNull PipelineState pipelineState,
				PipelineResult currentResult
		);

		<S> void onGroupCompleted(
				@NotNull PipelineGroup<S> group,
				@NotNull S state,
				@NotNull GroupOutcome outcome,
				PipelineResult currentResult
		);
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
		private PipelineResult result;
		private boolean stopped;
	}
}
