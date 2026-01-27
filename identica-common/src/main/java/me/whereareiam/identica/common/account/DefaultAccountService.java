package me.whereareiam.identica.common.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.ProviderProfilePersistenceService;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.UsernameHistoryEntry;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.account.AccountDecision;
import me.whereareiam.identica.model.account.AccountPreparation;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.registry.IdentityRegistry;
import me.whereareiam.identica.service.AccountService;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.util.EventUtil;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultAccountService implements AccountService {
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;
	private final UsernameHistoryPersistenceService usernameHistoryPersistenceService;
	private final IdentityRegistry identityRegistry;
	private final ProviderManager providerManager;

	@Override
	public @NotNull AccountPreparation prepareAccount(
			@NotNull AccountProviderProfile profile,
			@Nullable UUID existingId
	) {
		String providerId = profile.getProviderId();
		String providerSubject = profile.getProviderSubject();
		String providerUsername = profile.getProviderUsername();

		if (providerId.isBlank())
			throw new IllegalArgumentException("providerId");
		if (providerSubject.isBlank())
			throw new IllegalArgumentException("providerSubject");
		if (providerUsername.isBlank())
			throw new IllegalArgumentException("providerUsername");

		long now = System.currentTimeMillis();

		AccountProviderLink existingLink = providerLinkPersistenceService
				.findBySubject(providerId, providerSubject)
				.orElse(null);
		UUID uniqueId = existingLink != null ? existingLink.getUniqueId() : existingId;
		if (uniqueId == null) uniqueId = UniqueIdGenerator.newIdenticaUniqueId();

		Account account = accountPersistenceService.findByUniqueId(uniqueId).orElse(null);
		boolean created = false;
		if (account == null) {
			account = Account.builder()
					.uniqueId(uniqueId)
					.username(providerUsername)
					.source(UsernameSource.PROVIDER)
					.createdAt(now)
					.lastSeenAt(now)
					.build();

			accountPersistenceService.create(account);
			created = true;
		} else {
			accountPersistenceService.updateLastSeen(uniqueId, now);
		}

		AccountProviderLink link = upsertProviderLink(existingLink, uniqueId, providerId, providerSubject, now);
		providerProfilePersistenceService.upsert(profile);

		String previousUsername = account.getUsername();
		UsernameSource previousSource = account.getSource();
		boolean usernameChanged = applyReplication(account, link, profile);

		AccountPrepareEvent preparedEvent = new AccountPrepareEvent(
				account.getUsername(),
				null,
				account,
				link,
				profile,
				created
		);
		EventUtil.callEvent(preparedEvent);
		AccountDecision decision = preparedEvent.getDecision();
		if (decision == null) decision = AccountDecision.allow();
		String effectiveUsername = preparedEvent.getEffectiveUsername();
		if (effectiveUsername == null || effectiveUsername.isBlank())
			effectiveUsername = account.getUsername();

		if (usernameChanged) {
			if (decision.isDenied()) {
				account.setUsername(previousUsername);
				account.setSource(previousSource);
			} else {
				persistUsernameChange(account, previousUsername, link.getProviderId(), account.getSource(), now);
			}
		}

		AccountPreparation.Provider provider = AccountPreparation.Provider.builder()
				.link(link)
				.profile(profile)
				.build();

		return AccountPreparation.builder()
				.account(account)
				.provider(provider)
				.decision(decision)
				.effectiveUsername(effectiveUsername)
				.created(created)
				.build();
	}

	@Override
	public @Nullable Session startSession(@NotNull AccountPreparation preparation, @Nullable String ip) {
		if (preparation.getDecision().isDenied()) return null;

		Account account = preparation.getAccount();
		AccountPreparation.Provider provider = preparation.getProvider();
		AccountProviderLink link = provider.getLink();
		AccountProviderProfile profile = provider.getProfile();

		String providerUsername = profile.getProviderUsername();
		String originalUsername = providerUsername.isBlank()
				? account.getUsername()
				: providerUsername;

		String effectiveUsername = preparation.getEffectiveUsername();
		if (effectiveUsername == null || effectiveUsername.isBlank())
			effectiveUsername = account.getUsername();

		Session session = Session.builder()
				.uniqueId(account.getUniqueId())
				.providerId(link.getProviderId())
				.providerSubject(link.getProviderSubject())
				.originalUsername(originalUsername)
				.effectiveUsername(effectiveUsername)
				.ip(ip)
				.createdAt(System.currentTimeMillis())
				.build();

		return identityRegistry.openSession(session).join();
	}

	private boolean applyReplication(
			@NotNull Account account,
			@NotNull AccountProviderLink link,
			@NotNull AccountProviderProfile profile
	) {
		String providerUsername = profile.getProviderUsername();
		if (providerUsername.isBlank())
			return false;

		if (account.getSource() == UsernameSource.MANUAL)
			return false;

		if (!link.isPrimary())
			return false;

		if (!isProviderAuthoritative(link.getProviderId()))
			return false;

		String candidate = providerUsername.trim();
		if (candidate.equals(account.getUsername()))
			return false;

		account.setUsername(candidate);
		account.setSource(UsernameSource.PROVIDER);

		return true;
	}

	private boolean isProviderAuthoritative(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return false;
		return providerManager.getProviders().stream()
				.map(InternalProvider::getDescriptor)
				.filter(descriptor -> descriptor != null && descriptor.getId() != null)
				.anyMatch(descriptor -> descriptor.getId().equalsIgnoreCase(providerId)
						&& descriptor.hasCapability(ProviderCapability.AUTHORITATIVE_USERNAME));
	}

	private void persistUsernameChange(
			@NotNull Account account,
			@NotNull String previousUsername,
			@Nullable String providerId,
			@NotNull UsernameSource source,
			long now
	) {
		String candidate = account.getUsername();
		if (candidate.isBlank()) return;
		if (previousUsername.equals(candidate)) return;

		accountPersistenceService.updateUsername(account.getUniqueId(), candidate);
		accountPersistenceService.updateUsernameSource(account.getUniqueId(), source);

		if (previousUsername.isBlank()) return;
		if (providerId != null && providerId.isBlank()) providerId = null;
		UsernameHistoryEntry entry = UsernameHistoryEntry.builder()
				.uniqueId(account.getUniqueId())
				.providerId(providerId)
				.oldUsername(previousUsername)
				.newUsername(candidate)
				.source(source.getId())
				.changedAt(now)
				.build();

		usernameHistoryPersistenceService.record(entry);
	}

	private AccountProviderLink upsertProviderLink(
			@Nullable AccountProviderLink existing,
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@NotNull String providerSubject,
			long now
	) {
		boolean primary = existing != null && existing.isPrimary();
		if (existing == null) {
			List<AccountProviderLink> links = providerLinkPersistenceService.findByUniqueId(uniqueId);
			primary = links.isEmpty();
		}

		AccountProviderLink link = AccountProviderLink.builder()
				.uniqueId(uniqueId)
				.providerId(providerId)
				.providerSubject(providerSubject)
				.primary(primary)
				.linkedAt(existing != null ? existing.getLinkedAt() : now)
				.lastSeenAt(now)
				.build();

		return providerLinkPersistenceService.upsert(link);
	}
}
