package me.whereareiam.identica.common.identity.account;

import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

	private DefaultRegistrationAccountService service(ReservationCache reservationCache) {
		ProviderLinkPersistenceService providerLinkPersistenceService = mock(ProviderLinkPersistenceService.class);
		SessionService sessionService = mock(SessionService.class);
		when(sessionService.findByProviderSubject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));

		Settings settings = new Settings();
		Settings.Connection connection = new Settings.Connection();
		connection.setReservationTtl(Duration.ofMinutes(1));
		settings.setConnection(connection);

		return new DefaultRegistrationAccountService(
				providerLinkPersistenceService,
				sessionService,
				reservationCache,
				() -> settings
		);
	}

	private ProfileRequest request(String username, String ip, String providerSubject) {
		return ProfileRequest.builder()
				.identity(new ConnectionIdentity(username, ip))
				.providerId("premium")
				.providerSubject(providerSubject)
				.build();
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
