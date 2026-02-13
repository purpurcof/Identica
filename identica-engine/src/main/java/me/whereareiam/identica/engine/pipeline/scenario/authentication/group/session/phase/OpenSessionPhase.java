package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.SessionState;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class OpenSessionPhase implements PipelinePhase<SessionState> {
	private final SessionService sessionService;
	private final Provider<Messages> messagesProvider;
	private final Provider<Settings> settingsProvider;

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

		AuthContext authContext = state.getAuthContext();
		Session session = state.getSession();
		if (authContext == null || session == null) {
			state.setResult(PipelineResult.failed(authenticationFailedMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null) {
			state.setResult(PipelineResult.failed(authenticationFailedMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		Settings.AuthenticationScenario scenario = settingsProvider.get()
				.getConnection()
				.getAuthentication();
		return sessionService.open(session, scenario.getSessionConcurrencyPolicy())
				.thenApply(openedSession -> {
					if (openedSession == null) {
						state.setResult(PipelineResult.denied(authenticationFailedMessage()));
						return PhaseResult.pass(state);
					}

					authContext.setIdenticaUniqueId(openedSession.getUniqueId());
					pipelineState.setScenario(authContext);
					pipelineState.removeItem(IdentityMetaItem.class);
					state.setResult(result);
					return PhaseResult.pass(state);
				});
	}

	private @NotNull String authenticationFailedMessage() {
		return joinMessage(messagesProvider.get().getConnection().getAuthentication().getAuthenticationFailed());
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

}
