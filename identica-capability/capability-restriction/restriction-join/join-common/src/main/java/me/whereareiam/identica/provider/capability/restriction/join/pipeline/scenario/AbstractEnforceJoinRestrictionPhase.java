package me.whereareiam.identica.provider.capability.restriction.join.pipeline.scenario;

import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.engine.pipeline.scenario.base.state.IdentityMetaItem;
import me.whereareiam.identica.model.config.Engine;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.AbstractGroupState;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.capability.restriction.RestrictionService;
import me.whereareiam.identica.provider.capability.restriction.join.JoinRestrictionType;
import me.whereareiam.identica.provider.capability.restriction.join.config.JoinRestrictionMessages;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionDecision;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionEvaluationRequest;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionSignal;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractEnforceJoinRestrictionPhase<C extends ScenarioContext, S extends AbstractGroupState> implements PipelinePhase<S> {
	private final RestrictionService restrictionService;
	private final ProviderOperations providerOperations;
	private final Provider<JoinRestrictionMessages> messagesProvider;
	private final Provider<Engine> engineProvider;

	@Override
	public @NotNull String id() {
		return "enforce-join-restriction";
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<S>> execute(@NotNull PipelineState pipelineState, @NotNull S state) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		C context = resolveContext(pipelineState, state);
		ProviderContext provider = resolveProvider(context, state);
		if (context == null || provider == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		if (allowResumeBypass(engineProvider.get(), pipelineState))
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		RestrictionDecision decision = restrictionService.evaluate(RestrictionEvaluationRequest.builder()
				.type(JoinRestrictionType.TYPE)
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.providerUsername(provider.getProviderUsername())
				.connectionUniqueId(context.getIdentityReference().getConnectionUniqueId())
				.ip(context.getIp())
				.origin(context.getIdentity().getOrigin())
				.build());
		if (decision.isAllowed()) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		state.setResult(PipelineResult.denied(restrictedMessage(provider.getProviderId(), decision.getAllow())));
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	protected abstract @Nullable C resolveContext(@NotNull PipelineState pipelineState, @NotNull S state);

	protected abstract @Nullable ProviderContext resolveProvider(@Nullable C context, @NotNull S state);

	protected abstract boolean allowResumeBypass(@NotNull Engine settings, @NotNull PipelineState pipelineState);

	protected final @Nullable IdentityMetaItem identityMeta(@NotNull PipelineState pipelineState) {
		return pipelineState.item(IdentityMetaItem.class).orElse(null);
	}

	private @NotNull String restrictedMessage(@NotNull String providerId, @NotNull Set<RestrictionSignal> allow) {
		String providerName = providerOperations.displayProviderName(providerId);
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("providerId", providerId);
		placeholders.put("providerName", providerName != null ? providerName : providerId);
		placeholders.put("allow", describeAllow(allow));

		String resolved = String.join("\n", messagesProvider.get().getDenied());
		var format = Serializer.getEngine().getPlaceholderFormat();
		for (Map.Entry<String, String> entry : placeholders.entrySet())
			resolved = resolved.replace(format.format(entry.getKey()), entry.getValue() == null ? "" : entry.getValue());

		return resolved;
	}

	private @NotNull String describeAllow(@NotNull Set<RestrictionSignal> allow) {
		if (allow.isEmpty()) return "NONE";
		return allow.stream()
				.map(RestrictionSignal::getId)
				.collect(Collectors.joining(", "));
	}
}
