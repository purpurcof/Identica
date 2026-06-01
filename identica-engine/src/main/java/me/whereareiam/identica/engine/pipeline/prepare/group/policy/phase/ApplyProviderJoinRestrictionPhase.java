package me.whereareiam.identica.engine.pipeline.prepare.group.policy.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.prepare.PrepareAccountCandidateItem;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecisionItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.prepare.PrepareGroupState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionDecision;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.restriction.ProviderJoinRestrictionService;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ApplyProviderJoinRestrictionPhase implements PipelinePhase<PrepareGroupState> {
	private final ProviderJoinRestrictionService restrictionService;
	private final ProviderOperations providerOperations;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "apply-provider-join-restriction";
	}

	@Override
	public int order() {
		return 50;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}

	@Override
	public boolean supports(@NotNull PipelineState pipelineState, @NotNull PrepareGroupState state) {
		return pipelineState.item(PrepareDecisionItem.class).isEmpty();
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<PrepareGroupState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull PrepareGroupState state
	) {
		if (state.getRequest() == null) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(new PrepareContextItem());
		ProviderContext provider = context.getProvider();
		if (provider == null || provider.getProviderId() == null || provider.getProviderId().isBlank())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		if (provider.getProviderSubject() == null || provider.getProviderSubject().isBlank()) {
			ProviderJoinRestrictionDecision decision = restrictionService.evaluate(provider.getProviderId());
			if (decision.isAllowed())
				return CompletableFuture.completedFuture(PhaseResult.pass(state));

			pipelineState.putItem(PrepareDecisionItem.deny(
					restrictedMessage(provider.getProviderId(), decision.getAllow()),
					context,
					null,
					state.getRequest().getIdentity().getUsername()
			), 0L);
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		PrepareAccountCandidateItem candidate = pipelineState.item(PrepareAccountCandidateItem.class).orElse(null);
		ProviderJoinRestrictionDecision decision = restrictionService.evaluate(
				provider.getProviderId(),
				provider.getProviderSubject(),
				provider.getProviderUsername(),
				state.getRequest().getIdentity().getIp(),
				state.getRequest().getIdentity().getOrigin()
		);
		if (decision.isAllowed())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		pipelineState.putItem(PrepareDecisionItem.deny(
				restrictedMessage(provider.getProviderId(), decision.getAllow()),
				context,
				candidate != null ? candidate.getUniqueId() : null,
				state.getRequest().getIdentity().getUsername()
		), 0L);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String restrictedMessage(
			@NotNull String providerId,
			@NotNull Set<ProviderJoinRestrictionCondition> allow
	) {
		return render(messagesProvider.get().getProviders().getProviderRestriction().getDenied(), providerId, allow);
	}

	private @NotNull String render(
			@NotNull java.util.List<String> lines,
			@NotNull String providerId,
			@NotNull Set<ProviderJoinRestrictionCondition> allow
	) {
		String providerName = providerOperations.displayProviderName(providerId);
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("providerId", providerId);
		placeholders.put("providerName", providerName != null ? providerName : providerId);
		placeholders.put("allow", describeAllow(allow));

        String resolved = String.join("\n", lines);
		var format = Serializer.getEngine().getPlaceholderFormat();
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			resolved = resolved.replace(format.format(entry.getKey()), entry.getValue() == null ? "" : entry.getValue());
		}
		
		return resolved;
	}

	private @NotNull String describeAllow(@NotNull Set<ProviderJoinRestrictionCondition> allow) {
		if (allow.isEmpty()) return "NONE";
		return allow.stream()
				.map(Enum::name)
				.collect(Collectors.joining(", "));
	}
}
