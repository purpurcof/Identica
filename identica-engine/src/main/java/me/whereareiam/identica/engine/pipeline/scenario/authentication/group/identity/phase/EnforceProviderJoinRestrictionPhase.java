package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.IdentityState;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionDecision;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.restriction.ProviderJoinRestrictionService;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EnforceProviderJoinRestrictionPhase implements PipelinePhase<IdentityState> {
	private final ProviderJoinRestrictionService restrictionService;
	private final ProviderOperations providerOperations;
	private final Provider<Messages> messagesProvider;
	private final Provider<Settings> settingsProvider;

	@Override
	public @NotNull String id() {
		return "enforce-provider-join-restriction";
	}

	@Override
	public int order() {
		return 225;
	}

	@Override
	public @NotNull Class<IdentityState> stateType() {
		return IdentityState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<IdentityState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull IdentityState state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		AuthContext context = pipelineState.getScenario(pipelineState.getPipelineType()) instanceof AuthContext authContext
				? authContext
				: null;
		ProviderContext provider = state.getProvider();
		if (context == null || provider == null) return CompletableFuture.completedFuture(PhaseResult.pass(state));
		if (allowResumeBypass(pipelineState)) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ProviderJoinRestrictionDecision decision = restrictionService.evaluate(
				provider.getProviderId(),
				provider.getProviderSubject(),
				provider.getProviderUsername(),
				context.getIp(),
				context.getIdentity().getOrigin()
		);
		if (decision.isAllowed()) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		state.setResult(PipelineResult.denied(restrictedMessage(provider.getProviderId(), decision.getAllow())));
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
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

		String resolved = String.join("\n", messagesProvider.get().getConnection().getProviderRestriction().getDenied());
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

	private boolean allowResumeBypass(@NotNull PipelineState pipelineState) {
		if (!settingsProvider.get().getConnection().getScenarios().getAuthentication().isAllowProviderRestrictionResumeBypass())
			return false;

		IdentityMetaItem meta = pipelineState.item(IdentityMetaItem.class).orElse(null);
		return meta != null && meta.isResumed();
	}
}
