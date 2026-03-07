package me.whereareiam.identica.engine.prepare.group;

import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.group.GroupOutcome;
import me.whereareiam.identica.pipeline.group.PipelineGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPrepareGroup implements PipelineGroup<PrepareGroupState> {
	@Override
	public @NotNull PrepareGroupState initializeState(
			@NotNull PipelineState pipelineState,
			@Nullable PipelineResult currentResult
	) {
		PrepareGroupState state = new PrepareGroupState();
		if (currentResult != null) state.setResult(currentResult);
		return state;
	}

	@Override
	public @NotNull GroupOutcome complete(@NotNull PipelineState pipelineState, @NotNull PrepareGroupState state) {
		PipelineResult result = state.getResult();
		return result != null ? GroupOutcome.forceStop(result) : GroupOutcome.none();
	}
}
