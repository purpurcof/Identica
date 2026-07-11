package me.whereareiam.identica.engine.pipeline.completion.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.AbstractPipelineGroupRegistry;
import me.whereareiam.identica.engine.pipeline.completion.group.context.CompletionContextGroup;
import me.whereareiam.identica.engine.pipeline.completion.group.context.phase.BuildCompletionContextPhase;
import me.whereareiam.identica.engine.pipeline.completion.group.context.phase.ResolveCompletionProviderPhase;
import me.whereareiam.identica.engine.pipeline.completion.group.context.phase.ResolveCompletionSessionPhase;
import me.whereareiam.identica.engine.pipeline.completion.group.step.CompletionStepGroup;
import me.whereareiam.identica.engine.pipeline.completion.group.step.phase.ExecuteCompletionStepsPhase;
import me.whereareiam.identica.model.pipeline.phase.PhasePlacement;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CompletionPipelineRegistry extends AbstractPipelineGroupRegistry implements PipelineRegistry {
	@Inject
	public CompletionPipelineRegistry(
			CompletionContextGroup contextGroup,
			CompletionStepGroup stepGroup,
			ResolveCompletionSessionPhase resolveCompletionSessionPhase,
			ResolveCompletionProviderPhase resolveCompletionProviderPhase,
			BuildCompletionContextPhase buildCompletionContextPhase,
			ExecuteCompletionStepsPhase executeCompletionStepsPhase
	) {
		register(contextGroup);
		register(stepGroup);

		registerPhase(contextGroup.id(), resolveCompletionSessionPhase, PhasePlacement.first());
		registerPhase(contextGroup.id(), resolveCompletionProviderPhase, PhasePlacement.after(resolveCompletionSessionPhase.id()));
		registerPhase(contextGroup.id(), buildCompletionContextPhase, PhasePlacement.last());

		registerPhase(stepGroup.id(), executeCompletionStepsPhase, PhasePlacement.first());
	}

	@Override
	protected @NotNull String phaseLabel() {
		return "completion pipeline phase";
	}
}
