package me.whereareiam.identica.engine.pipeline.handshake.group.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.handshake.HandshakePolicy;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.engine.pipeline.handshake.state.HandshakeState;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class EvaluatePoliciesPhase implements PipelinePhase<HandshakeState> {
	private final HandshakeStore handshakeStore;

	@Override
	public @NotNull String id() {
		return "evaluate-policies";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<HandshakeState> stateType() {
		return HandshakeState.class;
	}

	@Override
	public boolean supports(
			@NotNull PipelineState pipelineState,
			@NotNull HandshakeState state
	) {
		HandshakeDecision decision = state.resolvedDecision();
		return decision.getStatus() == HandshakeDecision.Status.ALLOW
				&& !handshakeStore.policies().isEmpty()
				&& state.getRequest() != null;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<HandshakeState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull HandshakeState state
	) {
		List<CompletionStage<HandshakeDecision>> evaluations = new ArrayList<>();
		for (HandshakePolicy policy : handshakeStore.policies())
			evaluations.add(evaluatePolicy(policy, state));

		CompletableFuture<?>[] futures = evaluations.stream()
				.map(CompletionStage::toCompletableFuture)
				.toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(futures)
				.thenApply(ignored -> mergeDecisions(state.resolvedDecision(), evaluations))
				.thenApply(merged -> {
					state.setDecision(merged);
					return PhaseResult.pass(state);
				});
	}

	private CompletionStage<HandshakeDecision> evaluatePolicy(
			@NotNull HandshakePolicy policy,
			@NotNull HandshakeState state
	) {
		try {
			CompletionStage<HandshakeDecision> stage = policy.evaluate(state.getRequest());
			if (stage == null)
				return CompletableFuture.completedFuture(HandshakeDecision.allow());

			return stage.handle((decision, error) -> {
				if (error != null) {
					Logger.severe("Handshake policy failed %s", error.fillInStackTrace());
					return HandshakeDecision.allow();
				}

				HandshakeDecision resolved = decision != null
						? decision
						: HandshakeDecision.allow();
				if (resolved.getStatus() != HandshakeDecision.Status.ALLOW)
					Logger.debug("Handshake policy %s returned %s",
							policy.getClass().getSimpleName(),
							resolved.getStatus());

				return resolved;
			});
		} catch (Exception e) {
			Logger.severe("Handshake policy failed %s", e.fillInStackTrace());
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}
	}

	private HandshakeDecision mergeDecisions(
			@NotNull HandshakeDecision base,
			@NotNull List<CompletionStage<HandshakeDecision>> evaluations
	) {
		for (CompletionStage<HandshakeDecision> evaluation : evaluations) {
			HandshakeDecision decision = evaluation.toCompletableFuture().getNow(HandshakeDecision.allow());
			if (decision == null || decision.getStatus() == null)
				continue;

			if (decision.getStatus() == HandshakeDecision.Status.DENY)
				return decision;
		}

		return base;
	}
}
