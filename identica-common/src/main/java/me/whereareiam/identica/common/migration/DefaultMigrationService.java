package me.whereareiam.identica.common.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.migration.MigrationCancel;
import me.whereareiam.identica.migration.MigrationConfirm;
import me.whereareiam.identica.migration.MigrationRequest;
import me.whereareiam.identica.migration.MigrationResult;
import me.whereareiam.identica.migration.MigrationService;
import me.whereareiam.identica.migration.MigrationStart;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.migration.MigrationPendingState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.migration.MigrationPrecheckContext;
import me.whereareiam.identica.provider.migration.MigrationPrecheckResult;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;
import me.whereareiam.identica.type.migration.MigrationCancelScope;
import me.whereareiam.identica.type.migration.MigrationInitiator;
import me.whereareiam.identica.type.migration.MigrationResultStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
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

	private final Map<UUID, PendingMigration> pending = new ConcurrentHashMap<>();

	@Override
	public @NotNull MigrationResult request(@NotNull MigrationRequest request) {
		UUID connectionUniqueId = request.getConnectionUniqueId();
		if (connectionUniqueId == null) return result(MigrationResultStatus.FAILED, null);

		PendingMigration existing = pending.get(connectionUniqueId);
		if (existing != null) {
			if (!isExpired(existing)) {
				return result(MigrationResultStatus.PENDING_EXISTS, null);
			}
			pending.remove(connectionUniqueId);
		}

		if (hasPendingMigration(connectionUniqueId)) return result(MigrationResultStatus.PENDING_EXISTS, null);

		String targetProviderId = normalize(request.getTargetProviderId());
		if (targetProviderId == null) return result(MigrationResultStatus.FAILED, null);

		UUID accountUniqueId = resolveAccountUniqueId(request.getAccountUniqueId(), connectionUniqueId);

		AccountProviderLink link = providerLinkPersistenceService
				.findByUniqueIdAndProviderId(accountUniqueId, targetProviderId)
				.orElse(null);
		if (link != null && link.isPrimary()) return result(MigrationResultStatus.ALREADY_PRIMARY, null);

		PendingMigration pendingMigration = new PendingMigration(
				connectionUniqueId,
				accountUniqueId,
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

		PendingMigration pendingMigration = pending.get(connectionUniqueId);
		if (pendingMigration == null) return result(MigrationResultStatus.NO_PENDING, null);

		if (isExpired(pendingMigration)) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.EXPIRED, null);
		}

		if (hasPendingMigration(connectionUniqueId)) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.PENDING_EXISTS, null);
		}

		MigrationPrecheckResult precheck = runPrechecks(pendingMigration);
		if (precheck != null && !precheck.isAllowed()) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.PRECHECK_DENIED, precheck.getMessage());
		}

		UUID accountUniqueId = pendingMigration.accountUniqueId();
		if (accountUniqueId == null)
			accountUniqueId = connectionUniqueId;

		String targetProviderId = pendingMigration.targetProviderId();
		AccountProviderLink link = providerLinkPersistenceService
				.findByUniqueIdAndProviderId(accountUniqueId, targetProviderId)
				.orElse(null);
		if (link != null && link.isPrimary()) {
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.ALREADY_PRIMARY, null);
		}

		String kickMessage = resolveKickMessage(confirm.getKickMessage(), precheck);

		if (link != null) {
			providerLinkPersistenceService.setPrimaryExclusive(accountUniqueId, targetProviderId);
			closeSession(accountUniqueId);
			disconnect(connectionUniqueId, pendingMigration.username(), kickMessage);
			pending.remove(connectionUniqueId);
			return result(MigrationResultStatus.PRIMARY_SET, null);
		}

		boolean stored = storePendingMigration(pendingMigration, accountUniqueId);
		if (!stored) return result(MigrationResultStatus.FAILED, null);

		closeSession(accountUniqueId);
		disconnect(connectionUniqueId, pendingMigration.username(), kickMessage);
		pending.remove(connectionUniqueId);

		return result(MigrationResultStatus.STARTED, null);
	}

	@Override
	public @NotNull MigrationResult start(@NotNull MigrationStart start) {
		UUID accountUniqueId = resolveAccountUniqueId(start.getAccountUniqueId(), start.getConnectionUniqueId());
		UUID connectionUniqueId = resolveAccountUniqueId(start.getConnectionUniqueId(), accountUniqueId);
		if (accountUniqueId == null) return result(MigrationResultStatus.FAILED, null);

		pending.remove(connectionUniqueId);
		if (hasPendingMigration(connectionUniqueId)) return result(MigrationResultStatus.PENDING_EXISTS, null);

		String targetProviderId = normalize(start.getTargetProviderId());
		if (targetProviderId == null) return result(MigrationResultStatus.FAILED, null);

		PendingMigration pendingMigration = new PendingMigration(
				connectionUniqueId,
				accountUniqueId,
				targetProviderId,
				resolveUsername(start.getUsername(), accountUniqueId),
				normalize(start.getIp()),
				start.getInitiator(),
				start.getInitiatorUniqueId(),
				System.currentTimeMillis()
		);

		MigrationPrecheckResult precheck = runPrechecks(pendingMigration);
		if (precheck != null && !precheck.isAllowed())
			return result(MigrationResultStatus.PRECHECK_DENIED, precheck.getMessage());

		AccountProviderLink link = providerLinkPersistenceService
				.findByUniqueIdAndProviderId(accountUniqueId, targetProviderId)
				.orElse(null);
		if (link != null && link.isPrimary())
			return result(MigrationResultStatus.ALREADY_PRIMARY, null);

		String kickMessage = resolveKickMessage(start.getKickMessage(), precheck);

		if (link != null) {
			providerLinkPersistenceService.setPrimaryExclusive(accountUniqueId, targetProviderId);
			closeSession(accountUniqueId);
			disconnect(connectionUniqueId, pendingMigration.username(), kickMessage);
			return result(MigrationResultStatus.PRIMARY_SET, null);
		}

		boolean stored = storePendingMigration(pendingMigration, accountUniqueId);
		if (!stored)
			return result(MigrationResultStatus.FAILED, null);

		closeSession(accountUniqueId);
		disconnect(connectionUniqueId, pendingMigration.username(), kickMessage);
		return result(MigrationResultStatus.STARTED, null);
	}

	@Override
	public @NotNull MigrationResult cancel(@NotNull MigrationCancel cancel) {
		UUID connectionUniqueId = cancel.getConnectionUniqueId();
		if (connectionUniqueId == null)
			return result(MigrationResultStatus.FAILED, null);

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

	private boolean storePendingMigration(@NotNull PendingMigration pendingMigration, @NotNull UUID accountUniqueId) {
		long ttlMs = settingsProvider.get().getConnection().getMigration().pipelineTtlMillis();
		if (ttlMs <= 0) return false;

		JourneyType flow = settingsProvider.get().getConnection().getMigration().getFlow();
		MigrationContext context = MigrationContext.builder()
				.connectionUniqueId(pendingMigration.connectionUniqueId())
				.identity(new ConnectionIdentity(
						pendingMigration.connectionUniqueId(),
						nonNull(pendingMigration.username()),
						pendingMigration.ip()
				))
				.targetProviderId(pendingMigration.targetProviderId())
				.accountUniqueId(accountUniqueId)
				.build();

		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(PipelineType.MIGRATION);
		pipelineState.setScenario(context);
		pipelineState.putItem(new MigrationPendingState(
				pendingMigration.targetProviderId(),
				pendingMigration.requestedAt(),
				pendingMigration.initiator(),
				pendingMigration.initiatorUniqueId()
		), ttlMs);
		pipelineState.putItem(new JourneyStateItem(flow, null, 0), ttlMs);

		PipelineStateReference reference = PipelineStateReference.from(context);
		pipelineStateStore.save(reference, pipelineState, ttlMs);

		return true;
	}

	private boolean hasPendingMigration(@NotNull UUID connectionUniqueId) {
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();
		PipelineState state = pipelineStateStore.find(reference).orElse(null);
		if (state == null) return false;

		return state.item(MigrationPendingState.class).isPresent();
	}

	private MigrationPrecheckResult runPrechecks(@NotNull PendingMigration pendingMigration) {
		InternalProvider provider = resolveProvider(pendingMigration.targetProviderId());
		Set<ProviderMigrationPrecheck> prechecks = provider != null ? provider.getMigrationPrechecks() : null;
		if (prechecks == null || prechecks.isEmpty())
			return MigrationPrecheckResult.allow();

		MigrationPrecheckContext context = MigrationPrecheckContext.builder()
				.connectionUniqueId(pendingMigration.connectionUniqueId())
				.accountUniqueId(pendingMigration.accountUniqueId())
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

	private void disconnect(@NotNull UUID connectionUniqueId, @Nullable String username, @Nullable String message) {
		String resolved = message != null ? message : "";
		Identity identity = identityService.find(connectionUniqueId).orElse(null);
		if (identity == null && username != null && !username.isBlank()) {
			identity = identityService.find(username).orElse(null);
		}

		if (identity == null) return;
		identity.disconnect(Serializer.serialize(identity, resolved));
	}

	private @NotNull MigrationResult result(@NotNull MigrationResultStatus status, @Nullable String message) {
		return MigrationResult.builder()
				.status(status)
				.message(message)
				.build();
	}

	private UUID resolveAccountUniqueId(@Nullable UUID accountUniqueId, @Nullable UUID fallback) {
		return accountUniqueId != null ? accountUniqueId : fallback;
	}

	private boolean isExpired(@NotNull PendingMigration pendingMigration) {
		long ttlMs = commandsProvider.get().getBehavior().getMigration().getConfirmTtl().toMillis();
		return ttlMs > 0 && pendingMigration.requestedAt() + ttlMs < System.currentTimeMillis();
	}

	private @Nullable String normalize(@Nullable String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private @NotNull String nonNull(@Nullable String value) {
		return value == null ? "" : value;
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

	private record PendingMigration(
			UUID connectionUniqueId,
			UUID accountUniqueId,
			String targetProviderId,
			String username,
			String ip,
			MigrationInitiator initiator,
			UUID initiatorUniqueId,
			long requestedAt
	) {
	}
}
