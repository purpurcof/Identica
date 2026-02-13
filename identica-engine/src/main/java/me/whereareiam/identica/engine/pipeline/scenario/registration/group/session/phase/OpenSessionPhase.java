package me.whereareiam.identica.engine.pipeline.scenario.registration.group.session.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityMetaItem;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.session.SessionState;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class OpenSessionPhase implements PipelinePhase<SessionState> {
	private final SessionService sessionService;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "open-session";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<SessionState> stateType() {
		return SessionState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<SessionState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull SessionState state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		RegistrationContext context = state.getContext();
		Session session = state.getSession();
		if (context == null || session == null) {
			state.setResult(PipelineResult.failed(registrationFailedMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		return sessionService.open(session)
				.thenApply(openedSession -> {
					if (openedSession == null) {
						state.setResult(PipelineResult.denied(registrationFailedMessage()));
						return PhaseResult.pass(state);
					}

					context.setIdenticaUniqueId(openedSession.getUniqueId());
					pipelineState.setScenario(context);
					pipelineState.removeItem(IdentityMetaItem.class);
					state.setResult(result);
					return PhaseResult.pass(state);
				});
	}

	private @NotNull String registrationFailedMessage() {
		return joinMessage(messagesProvider.get().getConnection().getRegistration().getRegistrationFailed());
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

}
