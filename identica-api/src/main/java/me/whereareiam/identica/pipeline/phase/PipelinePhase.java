package me.whereareiam.identica.pipeline.phase;

import me.whereareiam.identica.model.pipeline.PipelineState;
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
