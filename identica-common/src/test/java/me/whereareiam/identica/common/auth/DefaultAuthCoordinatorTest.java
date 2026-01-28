package me.whereareiam.identica.common.auth;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import me.whereareiam.identica.auth.AuthenticationService;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.common.auth.handshake.HandshakeInstructionRegistry;
import me.whereareiam.identica.common.identity.DefaultIdentityService;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.ProviderProfilePersistenceService;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import me.whereareiam.identica.model.auth.AuthContext.Provider;
import me.whereareiam.identica.model.auth.request.LoginRequest;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.registry.IdentityRegistry;
import me.whereareiam.identica.util.EventUtil;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.session.SessionService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthCoordinatorTest {
	@Mock
	private Registry<HandshakePolicy> handshakePolicies;

	@Test
	void createsAccountWhenMissing() {
		UUID identicaId = UUID.randomUUID();
		AuthenticationService authenticationService = new TestAuthenticationService(context -> {
			context.setProvider(AuthContext.Provider.builder()
					.providerId("premium")
					.providerSubject("subject")
					.providerUsername("Steve")
					.build());
			return StepResult.complete(context);
		});

		EventManager eventManager = mock(EventManager.class);
		HandshakeInstructionRegistry instructionStore = mock(HandshakeInstructionRegistry.class);
		AccountPersistenceService accountPersistenceService = mock(AccountPersistenceService.class);
		ProviderLinkPersistenceService linkPersistence = mock(ProviderLinkPersistenceService.class);
		ProviderProfilePersistenceService profilePersistence = mock(ProviderProfilePersistenceService.class);
		UsernameHistoryPersistenceService usernameHistoryPersistenceService = mock(UsernameHistoryPersistenceService.class);
		ProviderManager providerManager = mock(ProviderManager.class);
		when(providerManager.getProviders()).thenReturn(List.of());
		IdentityRegistry identityRegistry = mock(IdentityRegistry.class);
		SessionService sessionService = mock(SessionService.class);
		when(sessionService.open(any()))
				.thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
		ReservationCache reservationCache = mock(ReservationCache.class);
		when(reservationCache.get(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		doAnswer(_ -> CompletableFuture.completedFuture(null)).when(reservationCache).put(anyString(), any(), anyLong());
		doAnswer(_ -> CompletableFuture.completedFuture(null)).when(reservationCache).invalidate(anyString());

		DefaultIdentityService identityService = new DefaultIdentityService(
				accountPersistenceService,
				linkPersistence,
				profilePersistence,
				usernameHistoryPersistenceService,
				identityRegistry,
				providerManager,
				sessionService,
				reservationCache,
				Settings::new
		);

		when(accountPersistenceService.findByUniqueId(identicaId)).thenReturn(Optional.empty());
		when(accountPersistenceService.create(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(linkPersistence.findBySubject("premium", "subject")).thenReturn(Optional.empty());
		when(linkPersistence.findByUniqueId(identicaId)).thenReturn(List.of());
		when(linkPersistence.upsert(any(AccountProviderLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(profilePersistence.findBySubject(anyString(), anyString())).thenReturn(Optional.empty());
		when(profilePersistence.upsert(any(AccountProviderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EventUtil.initialize(eventManager);
		DefaultAuthCoordinator coordinator = new DefaultAuthCoordinator(
				authenticationService,
				instructionStore,
				handshakePolicies,
				Messages::new,
				identityService
		);

		LoginRequest request = LoginRequest.builder()
				.connectionInfo(ConnectionInfo.builder()
						.identity(new OfflineIdentity(identicaId, "Steve", "127.0.0.1"))
						.onlineMode(true)
						.build())
				.connectionUniqueId(UUID.randomUUID())
				.build();

		coordinator.authenticate(request);

		verify(accountPersistenceService).create(any(Account.class));
		verify(linkPersistence).upsert(any(AccountProviderLink.class));
		verify(profilePersistence).upsert(any(AccountProviderProfile.class));
		verify(sessionService).open(any());
	}

	@Test
	void skipsCreateWhenAccountExists() {
		UUID identicaId = UUID.randomUUID();
		AuthenticationService authenticationService = new TestAuthenticationService(context -> {
			context.setProvider(Provider.builder()
					.providerId("premium")
					.providerSubject("subject")
					.providerUsername("Steve")
					.build());
			return StepResult.complete(context);
		});

		EventManager eventManager = mock(EventManager.class);
		HandshakeInstructionRegistry instructionStore = mock(HandshakeInstructionRegistry.class);
		AccountPersistenceService accountPersistenceService = mock(AccountPersistenceService.class);
		ProviderLinkPersistenceService linkPersistence = mock(ProviderLinkPersistenceService.class);
		ProviderProfilePersistenceService profilePersistence = mock(ProviderProfilePersistenceService.class);
		UsernameHistoryPersistenceService usernameHistoryPersistenceService = mock(UsernameHistoryPersistenceService.class);
		ProviderManager providerManager = mock(ProviderManager.class);
		when(providerManager.getProviders()).thenReturn(List.of());
		IdentityRegistry identityRegistry = mock(IdentityRegistry.class);
		SessionService sessionService = mock(SessionService.class);
		when(sessionService.open(any()))
				.thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
		ReservationCache reservationCache = mock(ReservationCache.class);
		when(reservationCache.get(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		doAnswer(_ -> CompletableFuture.completedFuture(null)).when(reservationCache).put(anyString(), any(), anyLong());
		doAnswer(_ -> CompletableFuture.completedFuture(null)).when(reservationCache).invalidate(anyString());

		DefaultIdentityService identityService = new DefaultIdentityService(
				accountPersistenceService,
				linkPersistence,
				profilePersistence,
				usernameHistoryPersistenceService,
				identityRegistry,
				providerManager,
				sessionService,
				reservationCache,
				Settings::new
		);

		when(accountPersistenceService.findByUniqueId(identicaId)).thenReturn(Optional.of(Account.builder()
				.uniqueId(identicaId)
				.username("Steve")
				.source(UsernameSource.PROVIDER)
				.createdAt(System.currentTimeMillis())
				.lastSeenAt(System.currentTimeMillis())
				.build()));
		when(linkPersistence.findBySubject("premium", "subject")).thenReturn(Optional.empty());
		when(linkPersistence.findByUniqueId(identicaId)).thenReturn(List.of());
		when(linkPersistence.upsert(any(AccountProviderLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(profilePersistence.findBySubject(anyString(), anyString())).thenReturn(Optional.empty());
		when(profilePersistence.upsert(any(AccountProviderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EventUtil.initialize(eventManager);
		DefaultAuthCoordinator coordinator = new DefaultAuthCoordinator(
				authenticationService,
				instructionStore,
				handshakePolicies,
				Messages::new,
				identityService
		);

		LoginRequest request = LoginRequest.builder()
				.connectionInfo(ConnectionInfo.builder()
						.identity(new OfflineIdentity(identicaId, "Steve", "127.0.0.1"))
						.onlineMode(true)
						.build())
				.connectionUniqueId(UUID.randomUUID())
				.build();

		coordinator.authenticate(request);

		verify(accountPersistenceService).updateLastSeen(eq(identicaId), anyLong());
		verify(linkPersistence).upsert(any(AccountProviderLink.class));
		verify(profilePersistence).upsert(any(AccountProviderProfile.class));
		verify(sessionService).open(any());
	}

	@RequiredArgsConstructor
	private static final class TestAuthenticationService implements AuthenticationService {
		private final Function<AuthContext, StepResult> responder;

		@Override
		public CompletableFuture<StepResult> authenticate(AuthContext context) {
			return CompletableFuture.completedFuture(responder.apply(context));
		}

		@Override
		public CompletableFuture<StepResult> resume(UUID connectionUniqueId) {
			return CompletableFuture.completedFuture(StepResult.noPending());
		}

		@Override
		public CompletableFuture<StepResult> resume(UUID connectionUniqueId, Consumer<AuthContext> contextUpdater) {
			return CompletableFuture.completedFuture(StepResult.noPending());
		}

		@Override
		public boolean hasPending(UUID connectionUniqueId) {
			return false;
		}

		@Override
		public boolean clearPending(UUID connectionUniqueId) {
			return false;
		}
	}

}
