package me.whereareiam.identica.provider.premium.policy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.handshake.HandshakePolicy;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.ProviderAttemptStore;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakeAttributes;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileSnapshot;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import me.whereareiam.identica.provider.premium.resolver.PremiumProfileLookup;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.util.UniqueIdGenerator;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumHandshakePolicy implements HandshakePolicy {
	private final AccountPersistenceService accountPersistenceService;
	private final PremiumProfileLookup profileLookup;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final PremiumProfileStore profileStore;
	private final ProviderAttemptStore attemptStore;
	private final Provider<Settings> settingsProvider;
	private final HandshakeStore handshakeStore;

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
			requestForceOnline(username, ip);
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		if (attemptStore.hasAttempt(PremiumConstants.PROVIDER_ID, PremiumConstants.ATTEMPT_SCOPE_VERIFY, username, ip)) {
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		if (hasPremiumLinkByProfileId(username) || hasPremiumLinkByUsername(username)) {
			requestForceOnline(username, ip);
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		Settings settings = settingsProvider.get();
		Settings.Connection connection = settings != null ? settings.getConnection() : null;
		Settings.Scenario scenario = connection != null ? connection.getAuthentication() : null;
		JourneyType preferredFlow = scenario != null ? scenario.getFlow() : null;
		if (preferredFlow == JourneyType.INTERACTIVE) {
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}

		return profileLookup.hasPremiumProfile(username)
				.thenApply(hasProfile -> {
					if (!hasProfile)
						return HandshakeDecision.allow();

					attemptStore.markAttempt(PremiumConstants.PROVIDER_ID, PremiumConstants.ATTEMPT_SCOPE_VERIFY, username, ip);
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
	}

	private boolean hasPremiumLinkByProfileId(String username) {
		PremiumProfileSnapshot snapshot = profileStore.find(username);

		String profileId = snapshot != null ? snapshot.getProfileId() : null;
		if (profileId == null || profileId.isBlank()) return false;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && profileId.equalsIgnoreCase(offlineUuid.toString())) return false;

		return providerLinkPersistenceService.findBySubject(PremiumConstants.PROVIDER_ID, profileId).isPresent();
	}

	private boolean hasPremiumLinkByUsername(String username) {
		List<Account> accounts = accountPersistenceService.findByUsername(username);
		if (accounts.isEmpty()) return false;
		if (accounts.size() > 1) return false;

		for (Account account : accounts) {
			if (account == null) continue;
			List<AccountProviderLink> links = providerLinkPersistenceService.findByUniqueId(account.getUniqueId());
			if (hasPremiumLink(links)) return true;
		}

		return false;
	}

	private boolean hasPremiumLink(List<AccountProviderLink> links) {
		if (links == null || links.isEmpty())
			return false;

		for (AccountProviderLink link : links) {
			if (link == null) continue;
			String providerId = link.getProviderId();
			if (providerId.equalsIgnoreCase(PremiumConstants.PROVIDER_ID))
				return true;
		}

		return false;
	}
}
