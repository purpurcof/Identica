package me.whereareiam.identica.common.uniqueid;

import me.whereareiam.identica.actor.OfflineIdentity;
import me.whereareiam.identica.common.uuid.UniqueIdResolver;
import me.whereareiam.identica.common.uuid.source.PendingBridgeUniqueIdResolutionSource;
import me.whereareiam.identica.common.uuid.source.PendingSubjectUniqueIdResolutionSource;
import me.whereareiam.identica.common.uuid.source.ProviderLinkUniqueIdResolutionSource;
import me.whereareiam.identica.common.uuid.source.SessionUniqueIdResolutionSource;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.resolution.UniqueIdResolutionSource;
import me.whereareiam.identica.session.PendingUniqueIdStore;
import me.whereareiam.identica.session.SessionStore;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UniqueIdResolverTest {
	private static final String ONLINE_PROVIDER_ID = "online";
	private static final String OFFLINE_PROVIDER_ID = "offline";

	@Test
	void resolvesFromSessionFirst() {
		UUID identicaId = UUID.randomUUID();
		String subject = UUID.randomUUID().toString();
		Session session = Session.builder()
				.uniqueId(identicaId)
				.providerId(ONLINE_PROVIDER_ID)
				.providerSubject(subject)
				.build();

		SessionStore sessionStore = mock(SessionStore.class);
		PendingUniqueIdStore pending = mock(PendingUniqueIdStore.class);
		ProviderLinkPersistenceService linkService = mock(ProviderLinkPersistenceService.class);

		when(sessionStore.findByProviderSubject(ONLINE_PROVIDER_ID, subject))
				.thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

		UniqueIdResolver service = new UniqueIdResolver(
				pending,
				this::settings,
				sources(sessionStore, linkService, pending)
		);

		UUID resolved = service.resolve(request(ONLINE_PROVIDER_ID, subject, true, subject));
		assertEquals(identicaId, resolved);
		verifyNoInteractions(linkService);
	}

	@Test
	void resolvesFromProviderLink() {
		UUID identicaId = UUID.randomUUID();
		String subject = UUID.randomUUID().toString();
		SessionStore sessionStore = mock(SessionStore.class);
		PendingUniqueIdStore pending = mock(PendingUniqueIdStore.class);
		ProviderLinkPersistenceService linkService = mock(ProviderLinkPersistenceService.class);

		when(sessionStore.findByProviderSubject(ONLINE_PROVIDER_ID, subject))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(linkService.findBySubject(ONLINE_PROVIDER_ID, subject))
				.thenReturn(Optional.of(AccountProviderLink.builder()
						.uniqueId(identicaId)
						.providerId(ONLINE_PROVIDER_ID)
						.providerSubject(subject)
						.primary(true)
						.linkedAt(System.currentTimeMillis())
						.lastSeenAt(System.currentTimeMillis())
						.build()));

		UniqueIdResolver service = new UniqueIdResolver(
				pending,
				this::settings,
				sources(sessionStore, linkService, pending)
		);

		UUID resolved = service.resolve(request(ONLINE_PROVIDER_ID, subject, true, subject));
		assertEquals(identicaId, resolved);
	}

	@Test
	void resolvesFromPendingSubject() {
		UUID pendingId = UUID.randomUUID();
		String subject = UUID.randomUUID().toString();
		SessionStore sessionStore = mock(SessionStore.class);
		PendingUniqueIdStore pending = mock(PendingUniqueIdStore.class);
		ProviderLinkPersistenceService linkService = mock(ProviderLinkPersistenceService.class);

		when(sessionStore.findByProviderSubject(ONLINE_PROVIDER_ID, subject))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(linkService.findBySubject(ONLINE_PROVIDER_ID, subject)).thenReturn(Optional.empty());
		when(pending.get("subject:" + ONLINE_PROVIDER_ID + ":" + subject.toLowerCase()))
				.thenReturn(CompletableFuture.completedFuture(Optional.of(pendingId)));

		UniqueIdResolver service = new UniqueIdResolver(
				pending,
				this::settings,
				sources(sessionStore, linkService, pending)
		);

		UUID resolved = service.resolve(request(ONLINE_PROVIDER_ID, subject, true, subject));
		assertEquals(pendingId, resolved);
	}

	@Test
	void resolvesFromBridge() {
		UUID pendingId = UUID.randomUUID();
		SessionStore sessionStore = mock(SessionStore.class);
		PendingUniqueIdStore pending = mock(PendingUniqueIdStore.class);
		ProviderLinkPersistenceService linkService = mock(ProviderLinkPersistenceService.class);
		String subject = offlineSubject("Steve");

		when(sessionStore.findByProviderSubject(OFFLINE_PROVIDER_ID, subject))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(linkService.findBySubject(OFFLINE_PROVIDER_ID, subject)).thenReturn(Optional.empty());
		when(pending.get("subject:" + OFFLINE_PROVIDER_ID + ":" + subject.toLowerCase()))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(pending.get("bridge:steve|127.0.0.1"))
				.thenReturn(CompletableFuture.completedFuture(Optional.of(pendingId)));

		UniqueIdResolver service = new UniqueIdResolver(
				pending,
				this::settings,
				sources(sessionStore, linkService, pending)
		);

		UUID resolved = service.resolve(request(OFFLINE_PROVIDER_ID, subject, false, null));
		assertEquals(pendingId, resolved);
	}

	@Test
	void generatesWhenNoMatch() {
		SessionStore sessionStore = mock(SessionStore.class);
		PendingUniqueIdStore pending = mock(PendingUniqueIdStore.class);
		ProviderLinkPersistenceService linkService = mock(ProviderLinkPersistenceService.class);
		String subject = offlineSubject("Steve");

		when(sessionStore.findByProviderSubject(OFFLINE_PROVIDER_ID, subject))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(linkService.findBySubject(OFFLINE_PROVIDER_ID, subject)).thenReturn(Optional.empty());
		when(pending.get(anyString()))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));

		UniqueIdResolver service = new UniqueIdResolver(
				pending,
				this::settings,
				sources(sessionStore, linkService, pending)
		);

		UUID resolved = service.resolve(request(OFFLINE_PROVIDER_ID, subject, false, null));
		assertNotNull(resolved);
		verify(pending, atLeastOnce()).put(anyString(), eq(resolved), anyLong());
	}

	@Test
	void returnsNullWhenOnlineSubjectMissing() {
		SessionStore sessionStore = mock(SessionStore.class);
		PendingUniqueIdStore pending = mock(PendingUniqueIdStore.class);
		ProviderLinkPersistenceService linkService = mock(ProviderLinkPersistenceService.class);

		UniqueIdResolver service = new UniqueIdResolver(
				pending,
				this::settings,
				sources(sessionStore, linkService, pending)
		);

		UUID resolved = service.resolve(request(ONLINE_PROVIDER_ID, null, true, null));
		assertNull(resolved);
		verifyNoInteractions(pending, linkService, sessionStore);
	}

	private Settings settings() {
		Settings settings = new Settings();
		Settings.Authentication authentication = new Settings.Authentication();
		authentication.setPendingUuidTtlMinutes(Duration.ofMinutes(15));
		settings.setAuthentication(authentication);
		return settings;
	}

	private ProfileRequest request(
			String providerId,
			String providerSubject,
			boolean onlineMode,
			String profileId
	) {
		String subject = providerSubject != null ? providerSubject : "";
		return ProfileRequest.builder()
				.connectionInfo(ConnectionInfo.builder()
						.identity(new OfflineIdentity("Steve", "127.0.0.1"))
						.onlineMode(onlineMode)
						.build())
				.providerId(providerId)
				.providerSubject(subject)
				.profileUniqueId(profileId)
				.build();
	}

	private String offlineSubject(String username) {
		UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
		return offlineUuid.toString();
	}

	private Set<UniqueIdResolutionSource> sources(
			SessionStore sessionStore,
			ProviderLinkPersistenceService linkService,
			PendingUniqueIdStore pending
	) {
		return Set.of(
				new SessionUniqueIdResolutionSource(sessionStore),
				new ProviderLinkUniqueIdResolutionSource(linkService),
				new PendingSubjectUniqueIdResolutionSource(pending),
				new PendingBridgeUniqueIdResolutionSource(pending)
		);
	}
}
