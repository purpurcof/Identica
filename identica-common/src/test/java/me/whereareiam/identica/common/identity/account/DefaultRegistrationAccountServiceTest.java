package me.whereareiam.identica.common.identity.account;

import me.whereareiam.identica.database.AccountReservationPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.type.identity.UniqueIdMode;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultRegistrationAccountServiceTest {
	@Test
	void reserveReusesExistingReservationForSameProviderSubject() {
		DefaultRegistrationAccountService service = service(new TestReservationCache());

		UUID first = service.reserve(request("Alpha", "1.1.1.1", "subject-a"));
		UUID second = service.reserve(request("Beta", "2.2.2.2", "subject-a"));

		assertEquals(first, second);
	}

	@Test
	void reserveDoesNotReuseReservationForSameUsernameAndIpWhenProviderSubjectDiffers() {
		DefaultRegistrationAccountService service = service(new TestReservationCache());

		UUID first = service.reserve(request("SharedName", "1.1.1.1", "subject-a"));
		UUID second = service.reserve(request("SharedName", "1.1.1.1", "subject-b"));

		assertNotEquals(first, second);
	}

	@Test
	void reserveReusesExistingProviderLinkUniqueId() {
		TestReservationCache reservationCache = new TestReservationCache();
		AccountReservationPersistenceService accountReservationPersistenceService = mock(AccountReservationPersistenceService.class);
		ProviderLinkPersistenceService providerLinkPersistenceService = mock(ProviderLinkPersistenceService.class);
		SessionService sessionService = mock(SessionService.class);
		UUID existingUniqueId = UUID.randomUUID();

		when(sessionService.findByProviderSubject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(providerLinkPersistenceService.findBySubject("premium", "subject-linked"))
				.thenReturn(Optional.of(AccountProviderLink.builder()
						.uniqueId(existingUniqueId)
						.providerId("premium")
						.providerSubject("subject-linked")
						.primaryLink(true)
						.build()));

		DefaultRegistrationAccountService service = service(
				reservationCache,
				accountReservationPersistenceService,
				providerLinkPersistenceService,
				sessionService
		);

		UUID reserved = service.reserve(request("MigratedPlayer", "1.1.1.1", "subject-linked"));

		assertEquals(existingUniqueId, reserved);
	}

	@Test
	void reserveReusesAccountReservationByUsername() {
		TestReservationCache reservationCache = new TestReservationCache();
		AccountReservationPersistenceService accountReservationPersistenceService = mock(AccountReservationPersistenceService.class);
		UUID clearedUniqueId = UUID.randomUUID();

		when(accountReservationPersistenceService.find("username:clearedplayer"))
				.thenReturn(Optional.of(clearedUniqueId));

		DefaultRegistrationAccountService service = service(
				reservationCache,
				accountReservationPersistenceService,
				mock(ProviderLinkPersistenceService.class),
				mockSessionService()
		);

		UUID reserved = service.reserve(request("ClearedPlayer", "1.1.1.1", "subject-new"));

		assertEquals(clearedUniqueId, reserved);
	}

	@Test
	void reserveUsesOfflineModeForNewAccountId() {
		DefaultRegistrationAccountService service = service(new TestReservationCache(), UniqueIdMode.OFFLINE);

		UUID reserved = service.reserve(request("OfflinePlayer", "1.1.1.1", "subject-new"));

		assertEquals(UniqueIdGenerator.offlinePlayerUniqueId("OfflinePlayer"), reserved);
	}

	@Test
	void reserveUsesPremiumModeProviderSubjectForNewAccountId() {
		DefaultRegistrationAccountService service = service(new TestReservationCache(), UniqueIdMode.PREMIUM);
		UUID providerUniqueId = UUID.randomUUID();

		UUID reserved = service.reserve(request("PremiumPlayer", "1.1.1.1", providerUniqueId.toString()));

		assertEquals(providerUniqueId, reserved);
	}

	@Test
	void reserveUsesPremiumModeObservedUniqueIdWhenSubjectIsNotUuid() {
		DefaultRegistrationAccountService service = service(new TestReservationCache(), UniqueIdMode.PREMIUM);
		UUID observedUniqueId = UUID.randomUUID();

		UUID reserved = service.reserve(request("PremiumPlayer", "1.1.1.1", "subject-new", observedUniqueId));

		assertEquals(observedUniqueId, reserved);
	}

	@Test
	void reserveDoesNotFallbackWhenPremiumModeHasNoPremiumUniqueId() {
		DefaultRegistrationAccountService service = service(new TestReservationCache(), UniqueIdMode.PREMIUM);

		UUID reserved = service.reserve(request("PremiumPlayer", "1.1.1.1", "subject-new"));

		assertNull(reserved);
	}

	@Test
	void reserveDoesNotUseOfflineObservedUniqueIdInPremiumMode() {
		DefaultRegistrationAccountService service = service(new TestReservationCache(), UniqueIdMode.PREMIUM);
		UUID offlineUniqueId = UniqueIdGenerator.offlinePlayerUniqueId("PremiumPlayer");

		UUID reserved = service.reserve(request("PremiumPlayer", "1.1.1.1", "subject-new", offlineUniqueId));

		assertNull(reserved);
	}

	private DefaultRegistrationAccountService service(ReservationCache reservationCache) {
		return service(reservationCache, UniqueIdMode.RANDOM);
	}

	private DefaultRegistrationAccountService service(ReservationCache reservationCache, UniqueIdMode uniqueIdMode) {
		AccountReservationPersistenceService accountReservationPersistenceService = mock(AccountReservationPersistenceService.class);
		ProviderLinkPersistenceService providerLinkPersistenceService = mock(ProviderLinkPersistenceService.class);
		SessionService sessionService = mockSessionService();
		return service(reservationCache, accountReservationPersistenceService, providerLinkPersistenceService, sessionService, uniqueIdMode);
	}

	private DefaultRegistrationAccountService service(
			ReservationCache reservationCache,
			AccountReservationPersistenceService accountReservationPersistenceService,
			ProviderLinkPersistenceService providerLinkPersistenceService,
			SessionService sessionService
	) {
		return service(
				reservationCache,
				accountReservationPersistenceService,
				providerLinkPersistenceService,
				sessionService,
				UniqueIdMode.RANDOM
		);
	}

	private DefaultRegistrationAccountService service(
			ReservationCache reservationCache,
			AccountReservationPersistenceService accountReservationPersistenceService,
			ProviderLinkPersistenceService providerLinkPersistenceService,
			SessionService sessionService,
			UniqueIdMode uniqueIdMode
	) {
		Settings settings = new Settings();
		Settings.Connection connection = new Settings.Connection();
		connection.setReservationTtl(Duration.ofMinutes(1));
		connection.setUniqueIdMode(uniqueIdMode);
		settings.setConnection(connection);

		return new DefaultRegistrationAccountService(
				accountReservationPersistenceService,
				providerLinkPersistenceService,
				sessionService,
				reservationCache,
				() -> settings,
				new UniqueIdGenerator(() -> settings)
		);
	}

	private ProfileRequest request(String username, String ip, String providerSubject) {
		return request(username, ip, providerSubject, null);
	}

	private ProfileRequest request(String username, String ip, String providerSubject, UUID observedUniqueId) {
		ConnectionIdentity identity = new ConnectionIdentity(username, ip);
		identity.setObservedUniqueId(observedUniqueId);

		return ProfileRequest.builder()
				.identity(identity)
				.providerId("premium")
				.providerSubject(providerSubject)
				.build();
	}

	private SessionService mockSessionService() {
		SessionService sessionService = mock(SessionService.class);
		when(sessionService.findByProviderSubject(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		return sessionService;
	}

	private static final class TestReservationCache implements ReservationCache {
		private final Map<String, UUID> values = new ConcurrentHashMap<>();

		@Override
		public @NotNull CompletableFuture<Optional<UUID>> get(String key) {
			return CompletableFuture.completedFuture(Optional.ofNullable(values.get(key)));
		}

		@Override
		public @NotNull CompletableFuture<Void> put(String key, UUID uuid, long ttlMs) {
			if (key != null && uuid != null)
				values.put(key, uuid);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NotNull CompletableFuture<Void> invalidate(String key) {
			if (key != null)
				values.remove(key);
			return CompletableFuture.completedFuture(null);
		}
	}
}
