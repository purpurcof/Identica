package me.whereareiam.identica.engine.pipeline.prepare.group.profile.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.event.scenario.migration.MigrationResolvedEvent;
import me.whereareiam.identica.type.ScenarioResolution;
import me.whereareiam.identica.engine.pipeline.prepare.group.PrepareGroupState;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.delivery.DeliveryPayload;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.model.delivery.DeliveryTarget;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.migration.MigrationPendingState;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.prepare.PrepareAccountCandidateItem;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecisionItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import me.whereareiam.identica.type.messaging.DeliverySemantics;
import me.whereareiam.identica.type.messaging.DeliverySource;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolvePendingMigrationAccountPhase implements PipelinePhase<PrepareGroupState> {
	private final PipelineStateStore pipelineStateStore;
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;
	private final DeliveryService deliveryService;

	@Override
	public @NotNull String id() {
		return "resolve-pending-migration-account";
	}

	@Override
	public int order() {
		return 150;
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
		if (request == null || provider == null) {
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String connectionKey = request.getConnectionKey();
		String providerId = provider.getProviderId();
		String providerSubject = provider.getProviderSubject();
		String requestedUsername = request.getIdentity().getUsername();
		if (connectionKey == null || connectionKey.isBlank()
				|| providerId == null || providerId.isBlank()
				|| providerSubject == null || providerSubject.isBlank()) {
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		PipelineState pendingState = pipelineStateStore.find(PipelineStateReference.builder()
				.connectionKey(connectionKey)
				.build()).orElse(null);
		if (pendingState == null || pendingState.getPipelineType() != PipelineType.MIGRATION)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		if (pendingState.item(MigrationPendingState.class).isEmpty())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		MigrationContext migration = (MigrationContext) pendingState.getScenario(PipelineType.MIGRATION);
		if (migration == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		String targetProviderId = migration.getTargetProviderId();
		if (targetProviderId == null) return CompletableFuture.completedFuture(PhaseResult.pass(state));
		if (!targetProviderId.equalsIgnoreCase(providerId)) {
			UUID cancelledAccountUniqueId = migration.getAccountUniqueId();
			if (cancelledAccountUniqueId != null) {
				deliveryService.queue(DeliveryRequest.builder()
						.id(UUID.randomUUID())
						.source(DeliverySource.NOTICE)
						.target(DeliveryTarget.builder()
								.accountUniqueId(cancelledAccountUniqueId)
								.build())
						.payload(DeliveryPayload.builder()
								.chatMessage("<green>ɪᴅᴇɴᴛɪᴄᴀ\n\n<white>Your pending migration was cancelled.</white>\n<white>You are still using your previous login provider.</white>\n\n<dark_gray>discord.arcadeya.com")
								.build())
						.checkpoint(DeliveryCheckpoint.PLATFORM_READY_INITIAL)
						.semantics(DeliverySemantics.ONCE)
						.createdAt(System.currentTimeMillis())
						.updatedAt(System.currentTimeMillis())
						.build());
			}

			UUID resolvedConnectionUniqueId = migration.getConnectionUniqueId();
			if (resolvedConnectionUniqueId != null) {
				EventUtil.callEvent(new MigrationResolvedEvent(migration, ScenarioResolution.CANCELLED, false));
			}

			pipelineStateStore.clear(PipelineStateReference.builder()
					.connectionKey(connectionKey)
					.build());
			Logger.debug(
					"Prepare cleared pending migration for provider mismatch requested=%s observed=%s key=%s",
					targetProviderId,
					providerId,
					connectionKey
			);
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		UUID accountUniqueId = migration.getAccountUniqueId();
		if (accountUniqueId == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		Optional<AccountProviderLink> storedLink = providerLinkPersistenceService.findBySubject(providerId, providerSubject);
		AccountProviderLink link = storedLink.orElseGet(() -> AccountProviderLink.builder()
				.uniqueId(accountUniqueId)
				.providerId(providerId)
				.providerSubject(providerSubject)
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
		AccountProviderProfile profile = providerProfilePersistenceService.findBySubject(providerId, providerSubject)
				.orElseGet(() -> AccountProviderProfile.builder()
						.providerId(providerId)
						.providerSubject(providerSubject)
						.providerUsername(requestedUsername)
						.build());

		boolean created = storedLink.isEmpty() || storedAccount == null;
		pipelineState.putItem(new PrepareAccountCandidateItem(
				accountUniqueId,
				null,
				account,
				link,
				profile,
				created
		), 0L);
		Logger.debug(
				"Prepare reusing pending migration account provider=%s identica=%s key=%s",
				providerId,
				accountUniqueId,
				connectionKey
		);

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
