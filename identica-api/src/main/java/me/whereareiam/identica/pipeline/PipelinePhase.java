package me.whereareiam.identica.pipeline;

import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;

public interface PipelinePhase<S> {
	@NotNull String id();

	int order();

	@NotNull Class<S> stateType();

	default boolean supports(@NotNull PipelineState pipelineState, @NotNull S state) {
		return true;
	}

	@NotNull CompletionStage<PhaseResult<S>> execute(@NotNull PipelineState pipelineState, @NotNull S state);
}
