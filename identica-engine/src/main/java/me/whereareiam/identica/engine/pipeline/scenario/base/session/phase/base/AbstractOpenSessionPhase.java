package me.whereareiam.identica.engine.pipeline.scenario.base.session.phase.base;

import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.base.state.IdentityMetaItem;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.AbstractGroupState;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
public abstract class AbstractOpenSessionPhase<C extends ScenarioContext, S extends AbstractGroupState> implements PipelinePhase<S> {
	private final SessionService sessionService;
	private final Provider<Messages> messagesProvider;
	private final EventManager eventManager;

	@Override
	public @NotNull String id() {
		return "open-session";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<S>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull S state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		C context = resolveContext(state);
		Session session = resolveSession(state);
		if (context == null || session == null) {
			state.setResult(PipelineResult.failed(failedMessage(messagesProvider.get())));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		return sessionService.open(session)
				.thenApply(openedSession -> {
					if (openedSession == null) {
						state.setResult(PipelineResult.denied(failedMessage(messagesProvider.get())));
						return PhaseResult.pass(state);
					}

					context.setAccountUniqueId(openedSession.getUniqueId());
					pipelineState.setScenario(context);
					pipelineState.removeItem(IdentityMetaItem.class);
					publishSessionOpened(context.getConnectionUniqueId(), openedSession);
					state.setResult(result);
					return PhaseResult.pass(state);
				});
	}

	protected abstract @NotNull PipelineType pipelineType();

	protected abstract @NotNull String failedMessage(@NotNull Messages messages);

	protected abstract @Nullable C resolveContext(@NotNull S state);

	protected abstract @Nullable Session resolveSession(@NotNull S state);

	private void publishSessionOpened(
			UUID connectionUniqueId,
			@NotNull Session session
	) {
		if (connectionUniqueId == null) return;
		eventManager.call(new SessionOpenedEvent(
				connectionUniqueId,
				pipelineType(),
				session
		));
	}
}
