package me.whereareiam.identica.engine.pipeline.handshake.group.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.handshake.state.HandshakeState;
import me.whereareiam.identica.event.handshake.HandshakeDecisionEvent;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class FinalizeDecisionPhase implements PipelinePhase<HandshakeState> {
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "finalize-decision";
	}

	@Override
	public int order() {
		return 300;
	}

	@Override
	public @NotNull Class<HandshakeState> stateType() {
		return HandshakeState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<HandshakeState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull HandshakeState state
	) {
		HandshakeDecision decision = state.resolvedDecision();
		HandshakeDecisionEvent event = new HandshakeDecisionEvent(state.getRequest(), decision);
		EventUtil.callEvent(event);

		HandshakeDecision finalDecision = event.getDecision();
		if (finalDecision == null)
			finalDecision = decision;

		if (finalDecision.getStatus() == HandshakeDecision.Status.DENY) {
			String message = finalDecision.getMessage();
			if (message == null || message.isBlank()) {
				finalDecision = HandshakeDecision.deny(joinMessage(messagesProvider.get()
						.getConnection()
						.getAuthentication()
						.getHandshakeDenied()));
			}
		}

		state.setDecision(finalDecision);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private String joinMessage(List<String> lines) {
		return String.join("\n", lines);
	}
}
