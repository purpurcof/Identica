package me.whereareiam.identica.pipeline;

import me.whereareiam.identica.model.pipeline.GroupOutcome;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PipelineGroup<S> {
	@NotNull String id();

	int order();

	@SuppressWarnings("unused")
	default boolean supports(@NotNull PipelineState state) {
		return true;
	}

	@NotNull Class<S> stateType();

	@NotNull S initializeState(@NotNull PipelineState pipelineState, @Nullable PipelineResult currentResult);

	@NotNull GroupOutcome complete(@NotNull PipelineState pipelineState, @NotNull S state);
}
