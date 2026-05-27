package me.whereareiam.identica.engine.pipeline.scenario.base.journey;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.GroupOutcome;
import me.whereareiam.identica.pipeline.PipelineGroup;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class JourneyGroup implements PipelineGroup<JourneyState> {
	private final Provider<Messages> messagesProvider;
	@Override
	public @NotNull String id() {
		return "journey";
	}

	@Override
	public int order() {
		return 300;
	}

	@Override
	public @NotNull Class<JourneyState> stateType() {
		return JourneyState.class;
	}

	@Override
	public @NotNull JourneyState initializeState(
			@NotNull PipelineState pipelineState,
			@Nullable PipelineResult currentResult
	) {
		JourneyState state = new JourneyState();
		state.setResult(currentResult);

		return state;
	}

	@Override
	public @NotNull GroupOutcome complete(@NotNull PipelineState pipelineState, @NotNull JourneyState state) {
		PipelineResult result = state.getResult();
		if (result == null) {
			return GroupOutcome.result(PipelineResult.failed(journeyMissingResultMessage(pipelineState)));
		}

		if (result.getStatus() == PipelineStatus.COMPLETE)
			pipelineState.removeItem(JourneyStateItem.class);

		return result.getStatus() == PipelineStatus.COMPLETE
				? GroupOutcome.result(result)
				: GroupOutcome.forceStop(result);
	}

	private @NotNull String journeyMissingResultMessage(@NotNull PipelineState pipelineState) {
		Messages.Connection.Scenario.Errors errors = resolveScenarioErrors(pipelineState);
		return String.join("\n", errors.getJourney().getMissingResult());
	}

	private @NotNull Messages.Connection.Scenario.Errors resolveScenarioErrors(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (pipelineType == PipelineType.REGISTRATION)
			return connection.getRegistration().getErrors();
		if (pipelineType == PipelineType.MIGRATION)
			return connection.getMigration().getErrors();

		return connection.getAuthentication().getErrors();
	}
}
