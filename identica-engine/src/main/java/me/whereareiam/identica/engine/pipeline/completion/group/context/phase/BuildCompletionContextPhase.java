package me.whereareiam.identica.engine.pipeline.completion.group.context.phase;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.completion.CompletionPipelineState;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class BuildCompletionContextPhase implements PipelinePhase<CompletionPipelineState> {
	@Override
	public @NotNull String id() {
		return "build-completion-context";
	}

	@Override
	public int order() {
		return 300;
	}

	@Override
	public @NotNull Class<CompletionPipelineState> stateType() {
		return CompletionPipelineState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<CompletionPipelineState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull CompletionPipelineState state
	) {
		Session session = state.getSession();
		if (session == null) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		CompletionContext context = CompletionContext.builder()
				.identity(state.getIdentity())
				.pipelineType(state.getPipelineType())
				.session(session)
				.provider(state.getProvider())
				.recognitionApplied(state.isRecognitionApplied())
				.build();
		state.setContext(context);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
