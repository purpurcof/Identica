package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.policy.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.policy.PolicyState;
import me.whereareiam.identica.model.UsernameHistoryEntry;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PersistUsernameChangePhase implements PipelinePhase<PolicyState> {
	private final AccountPersistenceService accountPersistenceService;
	private final UsernameHistoryPersistenceService usernameHistoryPersistenceService;

	@Override
	public @NotNull String id() {
		return "persist-username-change";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<PolicyState> stateType() {
		return PolicyState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<PolicyState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull PolicyState state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		IdentityMetaItem identity = pipelineState.item(IdentityMetaItem.class).orElse(null);
		AuthContext context = resolveContext(pipelineState);
		if (identity == null || context == null || context.getIdenticaUniqueId() == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		IdentityMetaItem.Change<String> usernameChange = identity.getUsername();
		IdentityMetaItem.Change<String> sourceChange = identity.getSource();
		if (!hasChange(usernameChange))
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		String candidate = usernameChange.getCurrent();
		if (candidate == null || candidate.isBlank())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		String sourceId = sourceChange != null ? sourceChange.getCurrent() : null;
		if (sourceId == null || sourceId.isBlank())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		accountPersistenceService.updateUsername(context.getIdenticaUniqueId(), candidate);
		accountPersistenceService.updateUsernameSource(
				context.getIdenticaUniqueId(),
				UsernameSource.fromId(sourceId)
		);

		String previous = usernameChange.getPrevious();
		if (previous == null || previous.isBlank())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ProviderContext provider = context.getProvider();
		String providerId = provider != null ? provider.getProviderId() : null;

		UsernameHistoryEntry entry = UsernameHistoryEntry.builder()
				.uniqueId(context.getIdenticaUniqueId())
				.providerId(providerId)
				.oldUsername(previous)
				.newUsername(candidate)
				.source(sourceId)
				.changedAt(System.currentTimeMillis())
				.build();

		usernameHistoryPersistenceService.record(entry);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private boolean hasChange(@Nullable IdentityMetaItem.Change<String> change) {
		return change != null && !Objects.equals(change.getPrevious(), change.getCurrent());
	}

	private AuthContext resolveContext(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null)
			return null;
		return pipelineState.getScenario(pipelineType) instanceof AuthContext authContext
				? authContext
				: null;
	}
}
