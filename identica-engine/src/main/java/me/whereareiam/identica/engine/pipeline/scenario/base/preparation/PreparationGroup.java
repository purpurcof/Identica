package me.whereareiam.identica.engine.pipeline.scenario.base.preparation;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.GroupOutcome;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.scenario.base.PreparationState;
import me.whereareiam.identica.pipeline.PipelineGroup;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PreparationGroup implements PipelineGroup<PreparationState> {
	private final Provider<Messages> messagesProvider;
	@Override
	public @NotNull String id() {
		return "preparation";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull Class<PreparationState> stateType() {
		return PreparationState.class;
	}

	@Override
	public @NotNull PreparationState initializeState(
			@NotNull PipelineState pipelineState,
			@Nullable PipelineResult currentResult
	) {
		PreparationState state = new PreparationState();
		ScenarioContext context = resolveScenarioContext(pipelineState);
		if (context != null)
			state.setContext(context);
		return state;
	}

	@Override
	public @NotNull GroupOutcome complete(@NotNull PipelineState pipelineState, @NotNull PreparationState state) {
		PipelineResult result = state.getResult();
		if (result != null) {
			return GroupOutcome.forceStop(result);
		}

		ScenarioContext resolved = state.getContext();
		if (resolved == null) {
			return GroupOutcome.forceStop(PipelineResult.failed(preparationMissingContextMessage(pipelineState)));
		}

		pipelineState.setScenario(resolved);
		return GroupOutcome.none();
	}

	private @Nullable ScenarioContext resolveScenarioContext(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null)
			return null;
		return pipelineState.getScenario(pipelineType);
	}

	private @NotNull String preparationMissingContextMessage(@NotNull PipelineState pipelineState) {
		Messages.Scenarios.Scenario.Errors errors = resolveScenarioErrors(pipelineState);
		return String.join("\n", errors.getPreparationMissingContext());
	}

	private @NotNull Messages.Scenarios.Scenario.Errors resolveScenarioErrors(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		Messages.Scenarios scenarios = messagesProvider.get().getScenarios();
		if (pipelineType == PipelineType.REGISTRATION) return scenarios.getRegistration().getErrors();
		if (pipelineType == PipelineType.MIGRATION) return scenarios.getMigration().getErrors();

		return scenarios.getAuthentication().getErrors();
	}
}
