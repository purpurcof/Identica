package me.whereareiam.identica.engine.pipeline.scenario.base.identity.phase.base;

import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.engine.pipeline.scenario.base.identity.item.IdentityMetaItem;
import me.whereareiam.identica.model.config.Engine;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.AbstractGroupState;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionDecision;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.restriction.ProviderJoinRestrictionService;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractEnforceProviderJoinRestrictionPhase<C extends ScenarioContext, S extends AbstractGroupState> implements PipelinePhase<S> {
	private final ProviderJoinRestrictionService restrictionService;
	private final ProviderOperations providerOperations;
	private final Provider<Messages> messagesProvider;
	private final Provider<Engine> engineProvider;

	@Override
	public @NotNull String id() {
		return "enforce-provider-join-restriction";
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<S>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull S state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		C context = resolveContext(pipelineState, state);
		ProviderContext provider = resolveProvider(context, state);
		if (context == null || provider == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		if (allowResumeBypass(engineProvider.get(), pipelineState))
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ProviderJoinRestrictionDecision decision = restrictionService.evaluate(
				provider.getProviderId(),
				provider.getProviderSubject(),
				provider.getProviderUsername(),
				context.getIp(),
				context.getIdentity().getOrigin()
		);
		if (decision.isAllowed())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		state.setResult(PipelineResult.denied(restrictedMessage(provider.getProviderId(), decision.getAllow())));
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	protected abstract @Nullable C resolveContext(@NotNull PipelineState pipelineState, @NotNull S state);

	protected abstract @Nullable ProviderContext resolveProvider(@Nullable C context, @NotNull S state);

	protected abstract boolean allowResumeBypass(@NotNull Engine settings, @NotNull PipelineState pipelineState);

	protected final @Nullable IdentityMetaItem identityMeta(@NotNull PipelineState pipelineState) {
		return pipelineState.item(IdentityMetaItem.class).orElse(null);
	}

	private @NotNull String restrictedMessage(
			@NotNull String providerId,
			@NotNull Set<ProviderJoinRestrictionCondition> allow
	) {
		String providerName = providerOperations.displayProviderName(providerId);
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("providerId", providerId);
		placeholders.put("providerName", providerName != null ? providerName : providerId);
		placeholders.put("allow", describeAllow(allow));

		String resolved = String.join("\n", messagesProvider.get().getProviders().getProviderRestriction().getDenied());
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
