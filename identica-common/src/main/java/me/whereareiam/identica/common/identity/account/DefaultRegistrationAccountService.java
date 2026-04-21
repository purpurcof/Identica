package me.whereareiam.identica.common.identity.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.identity.account.RegistrationAccountService;
import me.whereareiam.identica.common.util.UniqueIdResolutionSupport;
import me.whereareiam.identica.database.AccountReservationPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultRegistrationAccountService implements RegistrationAccountService {
	private final AccountReservationPersistenceService accountReservationPersistenceService;
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

		return resolveFirst(List.of(
				() -> resolveFromSession(providerId, providerSubject),
				() -> resolveFromProviderLink(providerId, providerSubject),
				() -> resolveFromReservation(providerId, providerSubject),
				() -> resolveFromAccountReservation(username),
				() -> reserveNewAccountId(providerId, providerSubject)
		));
	}

	@Override
	public void clearReservation(@NotNull ProfileRequest request) {
		String providerId = request.getProviderId();
		String providerSubject = request.getProviderSubject();

		String subjectKey = UniqueIdResolutionSupport.buildSubjectKey(providerId, providerSubject);
		if (subjectKey != null)
			reservationCache.invalidate(subjectKey).join();
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
	private UUID resolveFromReservation(String providerId, String providerSubject) {
		String subjectKey = UniqueIdResolutionSupport.buildSubjectKey(providerId, providerSubject);
		if (subjectKey != null) {
			return reservationCache.get(subjectKey)
					.thenApply(opt -> opt.orElse(null))
					.join();
		}

		return null;
	}

	private UUID reserveNewAccountId(String providerId, String providerSubject) {
		UUID generated = UniqueIdGenerator.newIdenticaUniqueId();
		long ttlMs = pendingTtlMillis();

		String subjectKey = UniqueIdResolutionSupport.buildSubjectKey(providerId, providerSubject);
		if (subjectKey != null)
			reservationCache.put(subjectKey, generated, ttlMs).join();

		return generated;
	}

	@Nullable
	private UUID resolveFromAccountReservation(String username) {
		String usernameKey = UniqueIdResolutionSupport.buildUsernameKey(username);
		if (usernameKey == null) return null;

		return accountReservationPersistenceService.find(usernameKey).orElse(null);
	}

	private long pendingTtlMillis() {
		Settings.Connection connection = settingsProvider.get().getConnection();

		Duration configured = connection.getReservationTtl();
		if (configured.isZero() || configured.isNegative())
			throw new IllegalStateException("settings.connection.reservationTtl must be positive");

		return configured.toMillis();
	}

	private @Nullable UUID resolveFirst(@NotNull List<Supplier<@Nullable UUID>> resolvers) {
		for (Supplier<@Nullable UUID> resolver : resolvers) {
			UUID resolved = resolver.get();
			if (resolved != null) return resolved;
		}

		return null;
	}
}
