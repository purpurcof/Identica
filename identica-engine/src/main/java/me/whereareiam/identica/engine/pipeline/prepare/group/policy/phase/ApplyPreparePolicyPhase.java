package me.whereareiam.identica.engine.pipeline.prepare.group.policy.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.prepare.group.PrepareGroupState;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.AccountDecision;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.prepare.PrepareAccountCandidateItem;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecisionItem;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ApplyPreparePolicyPhase implements PipelinePhase<PrepareGroupState> {
	private final EventManager eventManager;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "apply-prepare-policy";
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
	public boolean supports(@NotNull PipelineState pipelineState, @NotNull PrepareGroupState state) {
		if (pipelineState.item(PrepareDecisionItem.class).isPresent()) return false;

		PrepareAccountCandidateItem candidate = pipelineState.item(PrepareAccountCandidateItem.class).orElse(null);
		return candidate != null
				&& candidate.getAccount() != null
				&& candidate.getLink() != null
				&& candidate.getProfile() != null;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<PrepareGroupState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull PrepareGroupState state
	) {
		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(new PrepareContextItem());
		PrepareAccountCandidateItem candidate = pipelineState.item(PrepareAccountCandidateItem.class).orElse(null);
		if (state.getRequest() == null || candidate == null
				|| candidate.getAccount() == null
				|| candidate.getLink() == null
				|| candidate.getProfile() == null) {
			state.setResult(PipelineResult.failed(preparePolicyMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String requestedUsername = state.getRequest().getIdentity().getUsername();
		var account = candidate.getAccount();
		var link = candidate.getLink();
		var profile = candidate.getProfile();

		AccountPrepareEvent event = new AccountPrepareEvent(
				requestedUsername,
				context.getProvider(),
				account,
				link,
				profile,
				null
		);
		eventManager.call(event);

		AccountDecision decision = event.getDecision();
		String effectiveUsername = event.getEffectiveUsername();
		if (effectiveUsername == null || effectiveUsername.isBlank())
			effectiveUsername = account.getUsername();
		if (effectiveUsername.isBlank())
			effectiveUsername = requestedUsername;

		candidate.setEffectiveUsername(effectiveUsername);
		pipelineState.putItem(candidate, 0L);
		if (decision != null && decision.isDenied()) {
			Logger.info("Prepare denied by account review username=%s provider=%s",
					requestedUsername,
					context.getProvider() != null ? context.getProvider().getProviderId() : null);
			pipelineState.putItem(PrepareDecisionItem.deny(
					decision.getMessage(),
					context,
					candidate.getUniqueId(),
					effectiveUsername
			), 0L);
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		Logger.debug("Prepare resolved username=%s effective=%s provider=%s account=%s",
				requestedUsername,
				effectiveUsername,
				context.getProvider() != null ? context.getProvider().getProviderId() : null,
				candidate.getUniqueId());
		pipelineState.putItem(PrepareDecisionItem.allow(
				context,
				candidate.getUniqueId(),
				effectiveUsername
		), 0L);

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String preparePolicyMissingMessage() {
		return String.join("\n", messagesProvider.get()
				.getConnection()
				.getPrepare()
				.getErrors()
				.getPreparePolicyMissing());
	}
}
