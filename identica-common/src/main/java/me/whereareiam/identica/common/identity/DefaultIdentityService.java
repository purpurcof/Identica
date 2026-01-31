package me.whereareiam.identica.common.identity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.uuid.UniqueIdResolutionSupport;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.ProviderProfilePersistenceService;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.event.identity.session.SessionClosedEvent;
import me.whereareiam.identica.event.identity.session.SessionOpenedEvent;
import me.whereareiam.identica.event.identity.session.SessionPrepareEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.registry.IdentityRegistry;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.UsernameHistoryEntry;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.account.AccountDecision;
import me.whereareiam.identica.model.account.AccountPreparation;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.IdentityState;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.session.SessionService;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.util.EventUtil;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultIdentityService implements IdentityService {
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;
	private final UsernameHistoryPersistenceService usernameHistoryPersistenceService;
	private final IdentityRegistry identityRegistry;
	private final ProviderManager providerManager;
	private final SessionService sessionService;
	private final ReservationCache reservationCache;
	private final Provider<Settings> settingsProvider;

	@Override
	public @Nullable UUID reserveIdentity(@NotNull ProfileRequest request) {
		String username = UniqueIdResolutionSupport.normalize(request.getUsername());
		if (username == null) return null;

		String providerId = UniqueIdResolutionSupport.normalize(request.getProviderId());
		String providerSubject = UniqueIdResolutionSupport.normalize(request.getProviderSubject());
		if (providerId == null || providerSubject == null) return null;

		UUID resolved = resolveFromSession(providerId, providerSubject);
		if (resolved == null)
			resolved = resolveFromProviderLink(providerId, providerSubject);
		if (resolved == null)
			resolved = resolveFromReservation(providerId, providerSubject, username, request.getIp());
		if (resolved == null)
			resolved = reserveNewIdentity(providerId, providerSubject, username, request.getIp());

		long ttlMs = pendingTtlMillis();
		long expiresAt = ttlMs > 0 ? System.currentTimeMillis() + ttlMs : 0;
		identityRegistry.registerReserved(resolved, request, expiresAt);

		return resolved;
	}

	@Override
	public void clearReservation(@NotNull ProfileRequest request) {
		String providerId = request.getProviderId();
		String providerSubject = request.getProviderSubject();
		String username = request.getUsername();
		String ip = request.getIp();

		String subjectKey = UniqueIdResolutionSupport.buildSubjectKey(providerId, providerSubject);
		if (subjectKey != null)
			reservationCache.invalidate(subjectKey).join();

		String bridgeKey = UniqueIdResolutionSupport.buildBridgeKey(username, ip);
		if (bridgeKey != null)
			reservationCache.invalidate(bridgeKey).join();

	}

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
	public @NotNull CompletableFuture<@Nullable Session> openSession(@Nullable Session session) {
		if (session == null) return CompletableFuture.completedFuture(null);
		SessionPrepareEvent openEvent = new SessionPrepareEvent(session);
		EventUtil.callEvent(openEvent);
		if (openEvent.isCancelled())
			return CompletableFuture.completedFuture(null);

		return sessionService.open(openEvent.getSession())
				.thenApply(stored -> {
					if (stored == null)
						return null;
					identityRegistry.attachAuthenticated(stored);
					EventUtil.callEvent(new SessionOpenedEvent(stored));
					return stored;
				});
	}

	@Override
	public @NotNull CompletableFuture<Void> closeSession(@Nullable UUID uniqueId) {
		IdentityState state = uniqueId != null ? identityRegistry.findState(uniqueId).orElse(null) : null;
		Session current = state != null ? state.getSession() : null;

		return sessionService.close(uniqueId)
				.thenRun(() -> {
					if (uniqueId != null) {
						identityRegistry.detachAuthenticated(uniqueId);
						EventUtil.callEvent(new SessionClosedEvent(uniqueId, current));
					}
				});
	}

	@Override
	public @NotNull CompletableFuture<Optional<Session>> findSession(@Nullable UUID uniqueId) {
		return sessionService.findByUniqueId(uniqueId);
	}

	@Override
	public @NotNull CompletableFuture<SessionService.Page> listSessions(int page, int pageSize) {
		return sessionService.list(page, pageSize);
	}

	@Override
	public @NotNull Optional<IdentityState> findState(@NotNull UUID uniqueId) {
		return identityRegistry.findState(uniqueId);
	}

	@Override
	public @NotNull Optional<IdentityState> findState(@NotNull String username) {
		return identityRegistry.findState(username);
	}

	@Override
	public @NotNull Collection<IdentityState> getStates() {
		return identityRegistry.getStates();
	}

	@Override
	public void addPlayer(@NotNull Identity identity) {
		identityRegistry.addPlayer(identity);
	}

	@Override
	public void removePlayer(@NotNull UUID uniqueId) {
		identityRegistry.removePlayer(uniqueId);
	}

	@Override
	public @NotNull Optional<Identity> findPlayer(@NotNull UUID uniqueId) {
		return identityRegistry.findPlayer(uniqueId);
	}

	@Override
	public @NotNull Optional<Identity> findPlayer(@NotNull String username) {
		return identityRegistry.findPlayer(username);
	}

	@Override
	public @NotNull Collection<Identity> getPlayers() {
		return identityRegistry.getPlayers();
	}

	private UUID resolveFromSession(String providerId, String providerSubject) {
		return sessionService.findByProviderSubject(providerId, providerSubject)
				.thenApply(opt -> opt.map(Session::getUniqueId).orElse(null))
				.join();
	}

	private UUID resolveFromProviderLink(String providerId, String providerSubject) {
		return providerLinkPersistenceService.findBySubject(providerId, providerSubject)
				.map(AccountProviderLink::getUniqueId)
				.orElse(null);
	}

	private UUID resolveFromReservation(String providerId, String providerSubject, String username, String ip) {
		String subjectKey = UniqueIdResolutionSupport.buildSubjectKey(providerId, providerSubject);
		if (subjectKey != null) {
			UUID subjectMatch = reservationCache.get(subjectKey)
					.thenApply(opt -> opt.orElse(null))
					.join();
			if (subjectMatch != null) return subjectMatch;
		}

		String bridgeKey = UniqueIdResolutionSupport.buildBridgeKey(username, ip);
		if (bridgeKey == null) return null;

		return reservationCache.get(bridgeKey)
				.thenApply(opt -> opt.orElse(null))
				.join();
	}

	private UUID reserveNewIdentity(String providerId, String providerSubject, String username, String ip) {
		UUID generated = UniqueIdGenerator.newIdenticaUniqueId();
		long ttlMs = pendingTtlMillis();

		String bridgeKey = UniqueIdResolutionSupport.buildBridgeKey(username, ip);
		if (bridgeKey != null)
			reservationCache.put(bridgeKey, generated, ttlMs).join();

		String subjectKey = UniqueIdResolutionSupport.buildSubjectKey(providerId, providerSubject);
		if (subjectKey != null)
			reservationCache.put(subjectKey, generated, ttlMs).join();

		return generated;
	}

	private long pendingTtlMillis() {
		Settings.Authentication auth = settingsProvider.get().getAuthentication();

		Duration configured = auth.getReservationTtl();
		if (configured.isZero() || configured.isNegative())
			throw new IllegalStateException("settings.authentication.reservationTtl must be positive");

		return configured.toMillis();
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
				.filter(Objects::nonNull)
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
