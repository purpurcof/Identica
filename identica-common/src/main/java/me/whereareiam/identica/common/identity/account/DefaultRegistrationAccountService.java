package me.whereareiam.identica.common.identity.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.identity.account.RegistrationAccountService;
import me.whereareiam.identica.common.uuid.UniqueIdResolutionSupport;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultRegistrationAccountService implements RegistrationAccountService {
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final SessionService sessionService;
	private final ReservationCache reservationCache;
	private final Provider<Settings> settingsProvider;

	@Override
	public @Nullable UUID reserve(@NotNull ProfileRequest request) {
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
			resolved = reserveNewAccountId(providerId, providerSubject, username, request.getIp());

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

	@Nullable
	private UUID resolveFromSession(String providerId, String providerSubject) {
		Optional<Session> found = sessionService.findByProviderSubject(providerId, providerSubject).join();
		return found.map(Session::getUniqueId).orElse(null);
	}

	@Nullable
	private UUID resolveFromProviderLink(String providerId, String providerSubject) {
		return providerLinkPersistenceService.findBySubject(providerId, providerSubject)
				.map(AccountProviderLink::getUniqueId)
				.orElse(null);
	}

	@Nullable
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

	private UUID reserveNewAccountId(String providerId, String providerSubject, String username, String ip) {
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
		Settings.Connection connection = settingsProvider.get().getConnection();

		Duration configured = connection.getReservationTtl();
		if (configured.isZero() || configured.isNegative())
			throw new IllegalStateException("settings.connection.reservationTtl must be positive");

		return configured.toMillis();
	}

}
