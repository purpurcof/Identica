package me.whereareiam.identica.engine.pipeline.completion.group.step.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.completion.CompletionPipelineState;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.pipeline.completion.step.CompletionStep;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExecuteCompletionStepsPhase implements PipelinePhase<CompletionPipelineState> {
	private final CompletionExtensionRegistry completionExtensionRegistry;

	@Override
	public @NotNull String id() {
		return "execute-completion-steps";
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
	public @NotNull CompletionStage<PhaseResult<CompletionPipelineState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull CompletionPipelineState state
	) {
		CompletionContext context = state.getContext();
		if (context == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		List<CompletionStep> steps = completionExtensionRegistry.resolve(context.getProviderId(), context.getPipelineType());
		for (CompletionStep step : steps) {
			if (step == null) continue;
			if (!step.shouldExecute(context)) continue;

			try {
				step.execute(context);
			} catch (Exception exception) {
				Logger.warn(
						"Completion step %s failed for provider=%s pipeline=%s: %s",
						step.getName(),
						context.getProviderId(),
						context.getPipelineType(),
						exception.getMessage()
				);
			}
		}

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
