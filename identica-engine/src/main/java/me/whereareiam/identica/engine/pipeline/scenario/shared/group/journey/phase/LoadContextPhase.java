package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.state.scenario.shared.JourneyState;
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
		state.setPending(pipelineState.item(JourneyStateItem.class).orElse(null));
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String failureMessage(@Nullable PipelineType pipelineType) {
		Messages.Scenarios scenarios = messagesProvider.get().getScenarios();
		if (pipelineType == PipelineType.REGISTRATION) return String.join("\n", scenarios.getRegistration().getRegistrationFailed());
		if (pipelineType == PipelineType.MIGRATION) return String.join("\n", scenarios.getMigration().getMigrationFailed());

		return String.join("\n", scenarios.getAuthentication().getAuthenticationFailed());
	}
}
