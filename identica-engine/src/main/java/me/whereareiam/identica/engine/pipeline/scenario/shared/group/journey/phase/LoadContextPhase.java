package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.JourneyState;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyPendingState;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoadContextPhase implements PipelinePhase<JourneyState> {
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "load-context";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull Class<JourneyState> stateType() {
		return JourneyState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<JourneyState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull JourneyState state
	) {
		if (state.getResult() != null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		PipelineType pipelineType = pipelineState.getPipelineType();
		ScenarioContext context = pipelineType != null ? pipelineState.getScenario(pipelineType) : null;
		if (pipelineType == null || context == null) {
			state.setResult(PipelineResult.failed(failureMessage(pipelineType)));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		state.setContext(context);
		state.setPending(pipelineState.item(JourneyPendingState.class).orElse(null));
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String failureMessage(@Nullable PipelineType pipelineType) {
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (pipelineType == PipelineType.REGISTRATION)
			return String.join("\n", connection.getRegistration().getRegistrationFailed());
		return String.join("\n", connection.getAuthentication().getAuthenticationFailed());
	}
}
