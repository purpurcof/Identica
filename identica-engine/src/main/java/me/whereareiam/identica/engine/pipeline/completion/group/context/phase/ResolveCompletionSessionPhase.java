package me.whereareiam.identica.engine.pipeline.completion.group.context.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.completion.CompletionPipelineState;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolveCompletionSessionPhase implements PipelinePhase<CompletionPipelineState> {
	private final SessionService sessionService;

	@Override
	public @NotNull String id() {
		return "resolve-completion-session";
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
		UUID identicaUniqueId = state.getPendingState().getIdenticaUniqueId();
		if (identicaUniqueId == null) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		Session session = sessionService.findByUniqueId(identicaUniqueId).join().orElse(null);
		state.setSession(session);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
