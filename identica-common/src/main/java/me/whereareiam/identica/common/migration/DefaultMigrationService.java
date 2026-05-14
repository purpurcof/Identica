package me.whereareiam.identica.common.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.migration.PendingMigration;
import me.whereareiam.identica.model.migration.operation.*;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.migration.MigrationPendingState;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.migration.MigrationPrecheckContext;
import me.whereareiam.identica.provider.migration.MigrationPrecheckResult;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;
import me.whereareiam.identica.service.MigrationService;
import me.whereareiam.identica.type.migration.MigrationCancelScope;
import me.whereareiam.identica.type.migration.MigrationInitiator;
import me.whereareiam.identica.type.migration.MigrationResultStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultMigrationService implements MigrationService {
	private final ProviderManager providerManager;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final AccountPersistenceService accountPersistenceService;
	private final PipelineStateStore pipelineStateStore;
	private final SessionService sessionService;
	private final IdentityService identityService;
	private final Provider<Settings> settingsProvider;
	private final Provider<Commands> commandsProvider;
	private final Provider<Messages> messagesProvider;

	private final Map<UUID, PendingConfirmationMigration> pending = new ConcurrentHashMap<>();

	@Override
	public @NotNull MigrationResult request(@NotNull MigrationRequest request) {
		UUID connectionUniqueId = request.getConnectionUniqueId();
		if (connectionUniqueId == null) return result(MigrationResultStatus.FAILED, null);

		PendingConfirmationMigration existing = pending.get(connectionUniqueId);
		if (existing != null) {
			if (!isExpired(existing)) {
				return result(MigrationResultStatus.PENDING_EXISTS, null);
			}
			pending.remove(connectionUniqueId);
		}

		if (hasPendingMigration(connectionUniqueId)) return result(MigrationResultStatus.PENDING_EXISTS, null);

		String targetProviderId = normalize(request.getTargetProviderId());
		if (targetProviderId == null) return result(MigrationResultStatus.FAILED, null);

		UUID accountUniqueId = resolveIdenticaUniqueId(request.getAccountUniqueId(), connectionUniqueId);

		AccountProviderLink link = providerLinkPersistenceService
				.findByUniqueIdAndProviderId(accountUniqueId, targetProviderId)
				.orElse(null);
		if (link != null && link.isPrimaryLink()) return result(MigrationResultStatus.ALREADY_PRIMARY, null);

		PendingConfirmationMigration pendingMigration = new PendingConfirmationMigration(
				accountUniqueId,
				connectionUniqueId,
				targetProviderId,
				normalize(request.getUsername()),
				normalize(request.getIp()),
				request.getInitiator(),
				request.getInitiatorUniqueId(),
				System.currentTimeMillis()
		);
		pending.put(connectionUniqueId, pendingMigration);

		return result(MigrationResultStatus.PENDING_CONFIRMATION, null);
	}

	@Override
	public @NotNull MigrationResult confirm(@NotNull MigrationConfirm confirm) {
		UUID connectionUniqueId = confirm.getConnectionUniqueId();
		if (connectionUniqueId == null)
			return result(MigrationResultStatus.FAILED, null);

		PendingConfirmationMigration pendingMigration = pending.get(connectionUniqueId);
		if (pendingMigration == null) return result(MigrationResultStatus.NO_PENDING, null);

		if (isExpired(pendingMigration)) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.EXPIRED, null);
		}

		if (hasPendingMigration(connectionUniqueId)) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.PENDING_EXISTS, null);
		}

		if (!isUsernameFree(pendingMigration.username(), pendingMigration.uniqueId())) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.PRECHECK_DENIED, migrationLockedMessage());
		}

		MigrationPrecheckResult precheck = runPrechecks(pendingMigration);
		if (precheck != null && !precheck.isAllowed()) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.PRECHECK_DENIED, precheck.getMessage());
		}

		UUID accountUniqueId = pendingMigration.uniqueId();
		if (accountUniqueId == null)
			accountUniqueId = connectionUniqueId;

		String targetProviderId = pendingMigration.targetProviderId();
		AccountProviderLink link = providerLinkPersistenceService
				.findByUniqueIdAndProviderId(accountUniqueId, targetProviderId)
				.orElse(null);
		if (link != null && link.isPrimaryLink()) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.ALREADY_PRIMARY, null);
		}

		String kickMessage = resolveKickMessage(confirm.getKickMessage(), precheck);

		boolean stored = storePendingMigration(pendingMigration, accountUniqueId);
		if (!stored) return result(MigrationResultStatus.FAILED, null);

		closeSession(accountUniqueId);
		disconnect(connectionUniqueId, kickMessage);
		pending.remove(connectionUniqueId);

		return result(MigrationResultStatus.STARTED, null);
	}

	@Override
	public @NotNull MigrationResult start(@NotNull MigrationStart start) {
		UUID accountUniqueId = resolveIdenticaUniqueId(start.getUniqueId(), start.getConnectionUniqueId());
		UUID connectionUniqueId = resolveIdenticaUniqueId(start.getConnectionUniqueId(), accountUniqueId);
		if (accountUniqueId == null) return result(MigrationResultStatus.FAILED, null);

		pending.remove(connectionUniqueId);
		if (hasPendingMigration(connectionUniqueId)) return result(MigrationResultStatus.PENDING_EXISTS, null);

		String targetProviderId = normalize(start.getTargetProviderId());
		if (targetProviderId == null) return result(MigrationResultStatus.FAILED, null);

		PendingConfirmationMigration pendingMigration = new PendingConfirmationMigration(
				accountUniqueId,
				connectionUniqueId,
				targetProviderId,
				resolveUsername(start.getUsername(), accountUniqueId),
				normalize(start.getIp()),
				start.getInitiator(),
				start.getInitiatorUniqueId(),
				System.currentTimeMillis()
		);

		if (!isUsernameFree(pendingMigration.username(), pendingMigration.uniqueId())) {
			return result(MigrationResultStatus.PRECHECK_DENIED, migrationLockedMessage());
		}

		MigrationPrecheckResult precheck = runPrechecks(pendingMigration);
		if (precheck != null && !precheck.isAllowed())
			return result(MigrationResultStatus.PRECHECK_DENIED, precheck.getMessage());

		AccountProviderLink link = providerLinkPersistenceService
				.findByUniqueIdAndProviderId(accountUniqueId, targetProviderId)
				.orElse(null);
		if (link != null && link.isPrimaryLink())
			return result(MigrationResultStatus.ALREADY_PRIMARY, null);

		String kickMessage = resolveKickMessage(start.getKickMessage(), precheck);

		boolean stored = storePendingMigration(pendingMigration, accountUniqueId);
		if (!stored)
			return result(MigrationResultStatus.FAILED, null);

		closeSession(accountUniqueId);
		disconnect(connectionUniqueId, kickMessage);
		return result(MigrationResultStatus.STARTED, null);
	}

	@Override
	public @NotNull MigrationResult cancel(@NotNull MigrationCancel cancel) {
		UUID connectionUniqueId = cancel.getConnectionUniqueId();
		if (connectionUniqueId == null) return result(MigrationResultStatus.FAILED, null);

		MigrationCancelScope scope = cancel.getScope() != null ? cancel.getScope() : MigrationCancelScope.CONFIRMATION;
		boolean removed = false;

		if (scope == MigrationCancelScope.CONFIRMATION || scope == MigrationCancelScope.ALL) {
			removed = pending.remove(connectionUniqueId) != null;
		}

		if (scope == MigrationCancelScope.PENDING || scope == MigrationCancelScope.ALL) {
			PipelineStateReference reference = PipelineStateReference.builder()
					.connectionUniqueId(connectionUniqueId)
					.build();
			PipelineState stored = pipelineStateStore.find(reference).orElse(null);
			if (stored != null && stored.item(MigrationPendingState.class).isPresent()) {
				pipelineStateStore.clear(reference);
				removed = true;
			}
		}

		return removed
				? result(MigrationResultStatus.CANCELLED, null)
				: result(MigrationResultStatus.NO_PENDING, null);
	}

	private boolean storePendingMigration(@NotNull PendingConfirmationMigration pendingMigration, @NotNull UUID accountUniqueId) {
		long ttlMs = settingsProvider.get().getConnection().getScenarios().getMigration().pipelineTtlMillis();
		if (ttlMs <= 0) return false;

		JourneyMode journeyMode = settingsProvider.get().getConnection().getScenarios().getMigration().getJourneyMode();
		MigrationContext context = MigrationContext.builder()
				.connectionUniqueId(pendingMigration.connectionUniqueId())
				.identity(new ConnectionIdentity(
						accountUniqueId,
						nonNull(pendingMigration.username()),
						pendingMigration.ip()
				))
				.targetProviderId(pendingMigration.targetProviderId())
				.build();
		context.setProvider(resolvePendingProviderContext(
				pendingMigration.targetProviderId(),
				pendingMigration.username()
		));

		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(PipelineType.MIGRATION);
		pipelineState.setScenario(context);
		pipelineState.putItem(new MigrationPendingState(
				pendingMigration.targetProviderId(),
				pendingMigration.requestedAt(),
				pendingMigration.initiator(),
				pendingMigration.initiatorUniqueId()
		), ttlMs);
		pipelineState.putItem(new JourneyStateItem(journeyMode, null, 0), ttlMs);

		PipelineStateReference reference = PipelineStateReference.from(context);
		pipelineStateStore.save(reference, pipelineState, ttlMs);
		Logger.debug(
				"Stored migration pending connection=%s identica=%s target=%s username=%s ip=%s journeyMode=%s",
				pendingMigration.connectionUniqueId(),
				accountUniqueId,
				pendingMigration.targetProviderId(),
				pendingMigration.username(),
				pendingMigration.ip(),
				journeyMode
		);

		return true;
	}

	private @Nullable ProviderContext resolvePendingProviderContext(
			@Nullable String targetProviderId,
			@Nullable String username
	) {
		String normalizedProviderId = normalize(targetProviderId);
		String normalizedUsername = normalize(username);
		if (normalizedProviderId == null || normalizedUsername == null)
			return null;

		InternalProvider targetProvider = resolveProvider(normalizedProviderId);
		if (targetProvider == null || targetProvider.getDescriptor() == null)
			return null;

		if (!targetProvider.getDescriptor().hasCapability(ProviderCapability.OFFLINE_MODE))
			return null;

		UUID offlineUniqueId = UniqueIdGenerator.offlinePlayerUniqueId(normalizedUsername);
		if (offlineUniqueId == null)
			return null;

		ProviderContext provider = ProviderContext.builder()
				.providerId(normalizedProviderId)
				.providerSubject(offlineUniqueId.toString())
				.providerUsername(normalizedUsername)
				.source(ProviderOrigin.AUTO)
				.build();
		Logger.debug(
				"Prepared offline migration provider context target=%s username=%s subject=%s",
				normalizedProviderId,
				normalizedUsername,
				offlineUniqueId
		);
		return provider;
	}

	private boolean hasPendingMigration(@NotNull UUID connectionUniqueId) {
		return findStartedPendingMigration(connectionUniqueId) != null;
	}

	private MigrationPrecheckResult runPrechecks(@NotNull PendingConfirmationMigration pendingMigration) {
		InternalProvider provider = resolveProvider(pendingMigration.targetProviderId());
		Set<ProviderMigrationPrecheck> prechecks = provider != null ? provider.getMigrationPrechecks() : null;
		if (prechecks == null || prechecks.isEmpty())
			return MigrationPrecheckResult.allow();

		MigrationPrecheckContext context = MigrationPrecheckContext.builder()
				.uniqueId(pendingMigration.uniqueId())
				.connectionUniqueId(pendingMigration.connectionUniqueId())
				.username(pendingMigration.username())
				.ip(pendingMigration.ip())
				.providerId(pendingMigration.targetProviderId())
				.initiator(pendingMigration.initiator())
				.initiatorUniqueId(pendingMigration.initiatorUniqueId())
				.build();

		String kickMessage = null;
		for (ProviderMigrationPrecheck precheck : prechecks) {
			if (precheck == null) continue;

			MigrationPrecheckResult result = precheck.precheck(context);
			if (!result.isAllowed()) return result;

			if (kickMessage == null && result.getKickMessage() != null && !result.getKickMessage().isBlank())
				kickMessage = result.getKickMessage();
		}

		return MigrationPrecheckResult.allow(kickMessage);
	}

	private @Nullable InternalProvider resolveProvider(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;

		for (InternalProvider provider : providerManager.getProviders()) {
			if (provider == null || provider.getDescriptor() == null)
				continue;

			String id = provider.getDescriptor().getId();
			if (id.equalsIgnoreCase(providerId))
				return provider;
		}
		return null;
	}

	private void closeSession(@Nullable UUID accountUniqueId) {
		if (accountUniqueId == null) return;

		sessionService.close(accountUniqueId).join();
	}

	private void disconnect(@NotNull UUID connectionUniqueId, @Nullable String message) {
		String resolved = message != null ? message : "";
		Identity identity = identityService.findByConnectionUniqueId(connectionUniqueId).orElse(null);
		if (identity == null) return;

		identity.disconnect(Serializer.serialize(identity, resolved));
	}

	private @NotNull MigrationResult result(@NotNull MigrationResultStatus status, @Nullable String message) {
		return MigrationResult.builder()
				.status(status)
				.message(message)
				.build();
	}

	private UUID resolveIdenticaUniqueId(@Nullable UUID accountUniqueId, @Nullable UUID fallback) {
		return accountUniqueId != null ? accountUniqueId : fallback;
	}

	private boolean isExpired(@NotNull PendingConfirmationMigration pendingMigration) {
		long ttlMs = commandsProvider.get().getBehavior().getMigration().getConfirmTtl().toMillis();
		return ttlMs > 0 && pendingMigration.requestedAt() + ttlMs < System.currentTimeMillis();
	}

	private boolean isUsernameFree(@Nullable String username, @Nullable UUID currentUniqueId) {
		String normalized = normalize(username);
		if (normalized == null) return true;

		for (Account account : accountPersistenceService.findByUsername(normalized)) {
			if (account == null) continue;
			if (currentUniqueId != null && currentUniqueId.equals(account.getUniqueId()))
				continue;

			String existing = account.getUsername();
			if (normalized.equalsIgnoreCase(existing)) return false;
		}

		return true;
	}

	private @NotNull String migrationLockedMessage() {
		return messagesProvider.get().getCommands().getMigration().getLocked();
	}

	private @Nullable String resolveUsername(@Nullable String username, @NotNull UUID accountUniqueId) {
		String normalized = normalize(username);
		if (normalized != null) return normalized;

		Account account = accountPersistenceService.findByUniqueId(accountUniqueId).orElse(null);
		if (account == null) return null;

		return normalize(account.getUsername());
	}

	private @Nullable String resolveKickMessage(@Nullable String requested, @Nullable MigrationPrecheckResult precheck) {
		if (precheck != null && precheck.getKickMessage() != null && !precheck.getKickMessage().isBlank())
			return precheck.getKickMessage();

		return normalize(requested);
	}

	private @Nullable String normalize(@Nullable String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private @NotNull String nonNull(@Nullable String value) {
		return value == null ? "" : value;
	}

	@Override
	public @NotNull Optional<PendingMigration> findPendingMigration(@NotNull UUID connectionUniqueId) {
		PendingMigration confirmation = findConfirmationPendingMigration(connectionUniqueId);
		if (confirmation != null) return Optional.of(confirmation);

		return Optional.ofNullable(findStartedPendingMigration(connectionUniqueId));
	}

	private @Nullable PendingMigration findConfirmationPendingMigration(@NotNull UUID connectionUniqueId) {
		PendingConfirmationMigration pendingMigration = pending.get(connectionUniqueId);
		if (pendingMigration == null) return null;

		if (isExpired(pendingMigration)) {
			pending.remove(connectionUniqueId);
			return null;
		}

		return PendingMigration.builder()
				.uniqueId(pendingMigration.uniqueId())
				.connectionUniqueId(pendingMigration.connectionUniqueId())
				.targetProviderId(pendingMigration.targetProviderId())
				.requestedAt(pendingMigration.requestedAt())
				.initiator(pendingMigration.initiator())
				.initiatorUniqueId(pendingMigration.initiatorUniqueId())
				.phase(PendingMigration.Phase.CONFIRMATION)
				.build();
	}

	private @Nullable PendingMigration findStartedPendingMigration(@NotNull UUID connectionUniqueId) {
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();
		PipelineState state = pipelineStateStore.find(reference).orElse(null);
		if (state == null) {
			Logger.debug("Migration pending lookup missed connection=%s", connectionUniqueId);
			return null;
		}

		MigrationPendingState pendingState = state.item(MigrationPendingState.class).orElse(null);
		if (pendingState == null) {
			Logger.debug(
					"Migration pending lookup found state without pending marker connection=%s pipeline=%s",
					connectionUniqueId,
					state.getPipelineType()
			);
			return null;
		}

		MigrationContext context = (MigrationContext) state.getScenario(PipelineType.MIGRATION);
		UUID uniqueId = null;
		UUID storedConnectionUniqueId = connectionUniqueId;
		String targetProviderId = pendingState.getTargetProviderId();
		if (context != null) {
			if (context.getConnectionUniqueId() != null)
				storedConnectionUniqueId = context.getConnectionUniqueId();
			targetProviderId = context.getTargetProviderId();
			uniqueId = context.getIdentity().getAccountUniqueId();
		}
		Logger.debug(
				"Migration pending lookup found connection=%s storedConnection=%s identica=%s target=%s",
				connectionUniqueId,
				storedConnectionUniqueId,
				uniqueId,
				targetProviderId
		);

		return PendingMigration.builder()
				.uniqueId(uniqueId)
				.connectionUniqueId(storedConnectionUniqueId)
				.targetProviderId(targetProviderId)
				.requestedAt(pendingState.getRequestedAt())
				.initiator(pendingState.getInitiator())
				.initiatorUniqueId(pendingState.getInitiatorUniqueId())
				.phase(PendingMigration.Phase.STARTED)
				.build();
	}

	private record PendingConfirmationMigration(
			UUID uniqueId,
			UUID connectionUniqueId,
			String targetProviderId,
			String username,
			String ip,
			MigrationInitiator initiator,
			UUID initiatorUniqueId,
			long requestedAt
	) {
	}
}
