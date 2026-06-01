package me.whereareiam.identica.engine.pipeline.completion.group.context;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.pipeline.GroupOutcome;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.completion.CompletionPipelineState;
import me.whereareiam.identica.pipeline.PipelineGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class CompletionContextGroup implements PipelineGroup<CompletionPipelineState> {
	@Override
	public @NotNull String id() {
		return "completion-context";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull Class<CompletionPipelineState> stateType() {
		return CompletionPipelineState.class;
	}

	@Override
	public @NotNull CompletionPipelineState initializeState(
			@NotNull PipelineState pipelineState,
			@Nullable PipelineResult currentResult
	) {
		throw new IllegalStateException("Completion pipeline state is initialized by CompletionPipeline");
	}

	@Override
	public @NotNull GroupOutcome complete(
			@NotNull PipelineState pipelineState,
			@NotNull CompletionPipelineState state
	) {
		return GroupOutcome.none();
	}
}
