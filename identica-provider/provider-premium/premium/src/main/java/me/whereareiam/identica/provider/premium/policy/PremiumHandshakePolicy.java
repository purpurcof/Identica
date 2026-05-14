package me.whereareiam.identica.provider.premium.policy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.handshake.policy.ProviderScopedHandshakePolicy;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.provider.ProviderAttemptStore;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakeAttributes;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileSnapshot;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import me.whereareiam.identica.provider.premium.resolver.PremiumProfileLookup;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumHandshakePolicy implements ProviderScopedHandshakePolicy {
	private final AccountPersistenceService accountPersistenceService;
	private final PremiumProfileLookup profileLookup;
	private final ProviderManager providerManager;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final PremiumProfileStore profileStore;
	private final ProviderAttemptStore attemptStore;
	private final Provider<Settings> settingsProvider;
	private final HandshakeStore handshakeStore;

	@Override
	public @NotNull String providerId() {
		return PremiumConstants.PROVIDER_ID;
	}

	@Override
	public CompletionStage<HandshakeDecision> evaluate(HandshakeRequest request) {
		String username = request != null
				? request.getIdentity().getUsername()
				: null;
		String ip = request != null
				? request.getIdentity().getIp()
				: null;

		if (username == null || username.isBlank())
			return CompletableFuture.completedFuture(HandshakeDecision.allow());

		ProviderContext provider = request.getProvider();
		if (provider != null && PremiumConstants.PROVIDER_ID.equalsIgnoreCase(provider.getProviderId())) {
			Logger.debug(
					"Premium handshake forcing online for provider context username=%s ip=%s subject=%s",
					username,
					ip,
					provider.getProviderSubject()
			);
			requestForceOnline(username, ip);
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		if (attemptStore.hasAttempt(PremiumConstants.PROVIDER_ID, PremiumConstants.ATTEMPT_SCOPE_VERIFY, username, ip)) {
			Logger.debug(
					"Premium handshake allowed existing verify attempt username=%s ip=%s",
					username,
					ip
			);
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		String preferredProviderId = resolvePreferredLinkedProviderId(username);
		if (preferredProviderId != null) {
			if (PremiumConstants.PROVIDER_ID.equalsIgnoreCase(preferredProviderId)) {
				Logger.debug(
						"Premium handshake forcing online for preferred provider username=%s ip=%s provider=%s",
						username,
						ip,
						preferredProviderId
				);
				requestForceOnline(username, ip);
			} else {
				Logger.debug(
						"Premium handshake allowed non-premium preferred provider username=%s ip=%s provider=%s",
						username,
						ip,
						preferredProviderId
				);
			}
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		Settings settings = settingsProvider.get();
		Settings.Connection connection = settings != null ? settings.getConnection() : null;
		Settings.Scenario scenario = connection != null ? connection.getScenarios().getAuthentication() : null;
		JourneyMode preferredJourneyMode = scenario != null ? scenario.getJourneyMode() : null;
		if (preferredJourneyMode == JourneyMode.INTERACTIVE) {
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		return profileLookup.hasPremiumProfile(username)
				.thenApply(hasProfile -> {
					if (!hasProfile)
						return HandshakeDecision.allow();

					attemptStore.markAttempt(PremiumConstants.PROVIDER_ID, PremiumConstants.ATTEMPT_SCOPE_VERIFY, username, ip);
					Logger.debug(
							"Premium handshake marked verify attempt after profile lookup username=%s ip=%s",
							username,
							ip
					);
					return requestAndAllow(username, ip);
				});
	}

	private HandshakeDecision requestAndAllow(String username, String ip) {
		requestForceOnline(username, ip);
		return HandshakeDecision.allow();
	}

	private void requestForceOnline(String username, String ip) {
		if (username == null || username.isBlank() || ip == null || ip.isBlank()) return;

		long ttlMillis = settingsProvider.get()
				.getConnection()
				.handshakeInstructionTtlMillis();

		HandshakeInstruction instruction = HandshakeInstruction.create(
				new ConnectionIdentity(username, ip),
				ttlMillis
		);
		instruction.setAttribute(PremiumHandshakeAttributes.FORCE_ONLINE, true);
		handshakeStore.putInstruction(instruction);
		Logger.debug(
				"Premium handshake queued force-online instruction username=%s ip=%s ttl=%d",
				username,
				ip,
				ttlMillis
		);
	}

	private String resolvePreferredLinkedProviderId(String username) {
		String linkedByProfileId = resolvePreferredProviderIdByProfileId(username);
		if (linkedByProfileId != null) return linkedByProfileId;

		return resolvePreferredProviderIdByUsername(username);
	}

	private String resolvePreferredProviderIdByProfileId(String username) {
		PremiumProfileSnapshot snapshot = profileStore.find(username);

		String profileId = snapshot != null ? snapshot.getProfileId() : null;
		if (profileId == null || profileId.isBlank()) return null;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && profileId.equalsIgnoreCase(offlineUuid.toString())) return null;

		AccountProviderLink premiumLink = providerLinkPersistenceService
				.findBySubject(PremiumConstants.PROVIDER_ID, profileId)
				.orElse(null);
		if (premiumLink == null) return null;

		return resolvePreferredProviderId(providerLinkPersistenceService.findByUniqueId(premiumLink.getUniqueId()));
	}

	private String resolvePreferredProviderIdByUsername(String username) {
		List<Account> accounts = accountPersistenceService.findByUsername(username);
		if (accounts.isEmpty()) return null;
		if (accounts.size() > 1) return null;

		for (Account account : accounts) {
			if (account == null) continue;
			List<AccountProviderLink> links = providerLinkPersistenceService.findByUniqueId(account.getUniqueId());
			String preferredProviderId = resolvePreferredProviderId(links);
			if (preferredProviderId != null) return preferredProviderId;
		}

		return null;
	}

	private String resolvePreferredProviderId(List<AccountProviderLink> links) {
		if (links == null || links.isEmpty()) return null;

		List<AccountProviderLink> candidates = links.stream()
				.filter(link -> link != null && !link.getProviderId().isBlank())
				.toList();
		if (candidates.isEmpty()) return null;

		List<AccountProviderLink> primaries = candidates.stream()
				.filter(AccountProviderLink::isPrimaryLink)
				.toList();
		List<AccountProviderLink> preferredScope = !primaries.isEmpty() ? primaries : candidates;

		return preferredScope.stream()
				.max(Comparator.comparingInt(this::providerPriority)
						.thenComparing(AccountProviderLink::getProviderId, String.CASE_INSENSITIVE_ORDER))
				.map(AccountProviderLink::getProviderId)
				.orElse(null);
	}

	private int providerPriority(AccountProviderLink link) {
		String providerId = link.getProviderId();
		if (providerId.isBlank()) return 0;

		for (InternalProvider provider : providerManager.getProviders()) {
			if (provider == null || provider.getDescriptor() == null) continue;
			if (provider.getDescriptor().getId().equalsIgnoreCase(providerId))
				return provider.getPriority();
		}

		return 0;
	}
}
