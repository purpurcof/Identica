package me.whereareiam.identica.engine.pipeline.prepare.group.profile.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.prepare.PrepareAccountCandidateItem;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecisionItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.prepare.PrepareGroupState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.type.UsernameSource;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolvePreparedAccountPhase implements PipelinePhase<PrepareGroupState> {
	private final PrepareStateStore prepareStateStore;
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;

	@Override
	public @NotNull String id() {
		return "resolve-prepared-account";
	}

	@Override
	public int order() {
		return 175;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}

	@Override
	public boolean supports(@NotNull PipelineState pipelineState, @NotNull PrepareGroupState state) {
		if (pipelineState.item(PrepareDecisionItem.class).isPresent()) return false;
		if (pipelineState.item(PrepareAccountCandidateItem.class).isPresent()) return false;

		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(null);
		if (context == null) return false;

		ProviderContext provider = context.getProvider();
		if (provider == null) return false;
		if (provider.getProviderId() == null || provider.getProviderId().isBlank()) return false;
		if (provider.getProviderSubject() == null || provider.getProviderSubject().isBlank()) return false;

		String connectionKey = state.getRequest() != null ? state.getRequest().getConnectionKey() : null;
		if (connectionKey == null || connectionKey.isBlank()) return false;

		return prepareStateStore.peek(connectionKey)
				.map(PrepareDecision::getAccountUniqueId)
				.isPresent();
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<PrepareGroupState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull PrepareGroupState state
	) {
		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(new PrepareContextItem());
		var request = state.getRequest();
		ProviderContext provider = context.getProvider();
		if (request == null || provider == null) {
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String connectionKey = request.getConnectionKey();
		UUID accountUniqueId = connectionKey == null || connectionKey.isBlank()
				? null
				: prepareStateStore.peek(connectionKey)
						.map(PrepareDecision::getAccountUniqueId)
						.orElse(null);
		if (accountUniqueId == null) {
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String requestedUsername = request.getIdentity().getUsername();
		Optional<AccountProviderLink> storedLink = providerLinkPersistenceService.findBySubject(
				provider.getProviderId(),
				provider.getProviderSubject()
		);
		AccountProviderLink link = storedLink.orElseGet(() -> AccountProviderLink.builder()
				.uniqueId(accountUniqueId)
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.primaryLink(true)
				.build());
		Account storedAccount = accountPersistenceService.findByUniqueId(accountUniqueId).orElse(null);
		Account account = storedAccount != null
				? storedAccount.toBuilder().username(requestedUsername).build()
				: Account.builder()
						.uniqueId(accountUniqueId)
						.username(requestedUsername)
						.source(UsernameSource.PROVIDER)
						.build();
		AccountProviderProfile profile = providerProfilePersistenceService.findBySubject(
				provider.getProviderId(),
				provider.getProviderSubject()
		).orElseGet(() -> AccountProviderProfile.builder()
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.providerUsername(requestedUsername)
				.build());

		pipelineState.putItem(new PrepareAccountCandidateItem(
				accountUniqueId,
				null,
				account,
				link,
				profile
		), 0L);

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
