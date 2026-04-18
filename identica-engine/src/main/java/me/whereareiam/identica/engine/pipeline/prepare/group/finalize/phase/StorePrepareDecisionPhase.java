package me.whereareiam.identica.engine.pipeline.prepare.group.finalize.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.engine.pipeline.prepare.group.PrepareGroupState;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.prepare.PrepareAccountCandidateItem;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecisionItem;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class StorePrepareDecisionPhase implements PipelinePhase<PrepareGroupState> {
	private final PrepareStateStore prepareStateStore;

	@Override
	public @NotNull String id() {
		return "store-prepare-decision";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<PrepareGroupState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull PrepareGroupState state
	) {
		var request = state.getRequest();
		if (request == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		PrepareDecisionItem decisionItem = pipelineState.item(PrepareDecisionItem.class).orElse(null);
		PrepareDecision decision = decisionItem != null ? decisionItem.toDecision() : null;
		if (decision == null) {
			PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(new PrepareContextItem());
			PrepareAccountCandidateItem candidate = pipelineState.item(PrepareAccountCandidateItem.class).orElse(null);
			String username = request.getIdentity().getUsername();
			PrepareDecisionItem resolvedDecisionItem = PrepareDecisionItem.allow(
					context,
					candidate != null ? candidate.getUniqueId() : null,
					candidate != null && candidate.getEffectiveUsername() != null
							? candidate.getEffectiveUsername()
							: username
			);
			decision = resolvedDecisionItem.toDecision();
			pipelineState.putItem(resolvedDecisionItem, 0L);
		}
		if (decision == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		String connectionKey = request.getConnectionKey();
		if (connectionKey != null && !connectionKey.isBlank())
			prepareStateStore.put(connectionKey, decision);

		UUID uniqueId = decision.getUniqueId();
		if (uniqueId != null) prepareStateStore.put(uniqueId, connectionKey, decision);

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
