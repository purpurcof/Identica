package me.whereareiam.identica.engine.pipeline.prepare.group.profile.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.ProviderProfilePersistenceService;
import me.whereareiam.identica.engine.pipeline.prepare.group.PrepareGroupState;
import me.whereareiam.identica.identity.account.RegistrationAccountService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.prepare.PrepareAccountCandidateItem;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecisionItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoadPrepareAccountPhase implements PipelinePhase<PrepareGroupState> {
	private final RegistrationAccountService registrationAccountService;
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;

	@Override
	public @NotNull String id() {
		return "load-prepare-account";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}

	@Override
	public boolean supports(@NotNull PipelineState pipelineState, @NotNull PrepareGroupState state) {
		if (pipelineState.item(PrepareDecisionItem.class).isPresent())
			return false;

		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(null);
		return context != null
				&& context.getProvider() != null
				&& context.getProvider().getProviderId() != null
				&& !context.getProvider().getProviderId().isBlank()
				&& context.getProvider().getProviderSubject() != null
				&& !context.getProvider().getProviderSubject().isBlank();
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<PrepareGroupState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull PrepareGroupState state
	) {
		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(new PrepareContextItem());
		var request = state.getRequest();
		ProviderContext provider = context.getProvider();
		if (request == null || provider == null
				|| provider.getProviderId() == null || provider.getProviderId().isBlank()
				|| provider.getProviderSubject() == null || provider.getProviderSubject().isBlank()) {
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String requestedUsername = request.getIdentity().getUsername();
		UUID identicaUniqueId = registrationAccountService.reserve(ProfileRequest.builder()
				.identity(request.getIdentity())
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.build());

		if (identicaUniqueId == null) {
			Logger.warn("Prepare reservation missing username=%s provider=%s",
					requestedUsername,
					provider.getProviderId());
			pipelineState.putItem(PrepareDecisionItem.allow(
					context,
					null,
					requestedUsername
			), 0L);

			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		Optional<AccountProviderLink> storedLink = providerLinkPersistenceService.findBySubject(
				provider.getProviderId(),
				provider.getProviderSubject()
		);
		AccountProviderLink link = storedLink.orElseGet(() -> AccountProviderLink.builder()
				.uniqueId(identicaUniqueId)
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.primaryLink(true)
				.build());
		Account storedAccount = accountPersistenceService.findByUniqueId(identicaUniqueId).orElse(null);
		Account account = storedAccount != null
				? storedAccount.toBuilder().username(requestedUsername).build()
				: Account.builder()
						.uniqueId(identicaUniqueId)
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

		boolean created = storedLink.isEmpty() || storedAccount == null;
		pipelineState.putItem(new PrepareAccountCandidateItem(
				identicaUniqueId,
				null,
				account,
				link,
				profile,
				created
		), 0L);
		if (created) {
			Logger.debug("Prepare using transient account username=%s provider=%s",
					requestedUsername,
					provider.getProviderId());
		}

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
