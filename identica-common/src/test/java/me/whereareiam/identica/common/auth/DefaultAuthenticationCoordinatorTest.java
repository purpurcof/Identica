package me.whereareiam.identica.common.auth;

import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.common.auth.handshake.HandshakeInstructionRegistry;
import me.whereareiam.identica.common.identity.account.DefaultAccountService;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.ProviderProfilePersistenceService;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.ReservationCache;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.AuthContext.Provider;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.auth.request.LoginRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.util.EventUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAuthenticationCoordinatorTest {
	@Mock
	private Registry<HandshakePolicy> handshakePolicies;

	@Test
	void createsAccountWhenMissing() {
		UUID identicaId = UUID.randomUUID();
		FlowCoordinator flowCoordinator = mockFlowCoordinator(context -> {
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
		SessionService sessionService = mock(SessionService.class);
		when(sessionService.open(any()))
				.thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
		ReservationCache reservationCache = mock(ReservationCache.class);

		DefaultAccountService accountService = new DefaultAccountService(
				accountPersistenceService,
				linkPersistence,
				profilePersistence,
				usernameHistoryPersistenceService,
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
		when(profilePersistence.upsert(any(AccountProviderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EventUtil.initialize(eventManager);
		DefaultAuthenticationCoordinator coordinator = new DefaultAuthenticationCoordinator(
				flowCoordinator,
				instructionStore,
				handshakePolicies,
				Messages::new,
				accountService,
				sessionService
		);

		LoginRequest request = LoginRequest.builder()
				.identity(new ConnectionIdentity(identicaId, "Steve", "127.0.0.1"))
				.connectionUniqueId(UUID.randomUUID())
				.build();

		coordinator.authenticate(request).toCompletableFuture().join();

		verify(accountPersistenceService).create(any(Account.class));
		verify(linkPersistence).upsert(any(AccountProviderLink.class));
		verify(profilePersistence).upsert(any(AccountProviderProfile.class));
		verify(sessionService).open(any());
	}

	@Test
	void skipsCreateWhenAccountExists() {
		UUID identicaId = UUID.randomUUID();
		FlowCoordinator flowCoordinator = mockFlowCoordinator(context -> {
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
		SessionService sessionService = mock(SessionService.class);
		when(sessionService.open(any()))
				.thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
		ReservationCache reservationCache = mock(ReservationCache.class);

		DefaultAccountService accountService = new DefaultAccountService(
				accountPersistenceService,
				linkPersistence,
				profilePersistence,
				usernameHistoryPersistenceService,
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
		when(profilePersistence.upsert(any(AccountProviderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EventUtil.initialize(eventManager);
		DefaultAuthenticationCoordinator coordinator = new DefaultAuthenticationCoordinator(
				flowCoordinator,
				instructionStore,
				handshakePolicies,
				Messages::new,
				accountService,
				sessionService
		);

		LoginRequest request = LoginRequest.builder()
				.identity(new ConnectionIdentity(identicaId, "Steve", "127.0.0.1"))
				.connectionUniqueId(UUID.randomUUID())
				.build();

		coordinator.authenticate(request).toCompletableFuture().join();

		verify(accountPersistenceService).updateLastSeen(eq(identicaId), anyLong());
		verify(linkPersistence).upsert(any(AccountProviderLink.class));
		verify(profilePersistence).upsert(any(AccountProviderProfile.class));
		verify(sessionService).open(any());
	}

	private static FlowCoordinator mockFlowCoordinator(Function<AuthContext, StepResult> responder) {
		FlowCoordinator flowCoordinator = mock(FlowCoordinator.class);
		when(flowCoordinator.authenticate(any())).thenAnswer(invocation -> {
			AuthContext context = invocation.getArgument(0);
			return CompletableFuture.completedFuture(responder.apply(context));
		});
		return flowCoordinator;
	}

}
