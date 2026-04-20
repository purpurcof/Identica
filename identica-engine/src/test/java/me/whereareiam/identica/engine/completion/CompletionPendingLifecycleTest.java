package me.whereareiam.identica.engine.completion;

import me.whereareiam.identica.engine.pipeline.completion.CompletionPendingLifecycle;
import me.whereareiam.identica.pipeline.completion.CompletionCoordinator;
import me.whereareiam.identica.pipeline.completion.CompletionPendingStore;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.routing.RoutingTargetReachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.routing.RoutingStateStore;
import me.whereareiam.identica.type.RoutingTargetType;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.type.pipeline.PipelineType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompletionPendingLifecycleTest {
	@Test
	void sessionOpenedStoresPendingCompletion() {
		CompletionPendingStore pendingStore = mock(CompletionPendingStore.class);
		CompletionCoordinator completionCoordinator = mock(CompletionCoordinator.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingStateStore routingStateStore = mock(RoutingStateStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionCoordinator,
				identityService,
				routingStateStore,
				eventManager
		);
		UUID connectionUniqueId = UUID.randomUUID();
		Session session = Session.builder()
				.uniqueId(UUID.randomUUID())
				.providerId("cracked")
				.providerSubject("player-one")
				.build();

		lifecycle.onSessionOpened(new SessionOpenedEvent(
				connectionUniqueId,
				PipelineType.AUTHENTICATION,
				session,
				true
		));

		verify(pendingStore).put(any(), any());
		verify(completionCoordinator, never()).consumeAndExecute(any());
	}

	@Test
	void identityAttachedConsumesPendingCompletionWithoutRoutingTarget() {
		CompletionPendingStore pendingStore = mock(CompletionPendingStore.class);
		CompletionCoordinator completionCoordinator = mock(CompletionCoordinator.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingStateStore routingStateStore = mock(RoutingStateStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionCoordinator,
				identityService,
				routingStateStore,
				eventManager
		);
		TestIdentity identity = new TestIdentity(UUID.randomUUID(), "PlayerOne");
		when(pendingStore.peek(identity.getUniqueId())).thenReturn(Optional.of(mock(me.whereareiam.identica.model.pipeline.completion.CompletionPendingState.class)));
		when(routingStateStore.peek(identity.getUniqueId())).thenReturn(Optional.empty());

		lifecycle.onIdentityAttached(new IdentityAttachedEvent(identity));

		verify(completionCoordinator).consumeAndExecute(identity);
	}

	@Test
	void identityAttachedDefersWhileCompletedRoutingTargetExists() {
		CompletionPendingStore pendingStore = mock(CompletionPendingStore.class);
		CompletionCoordinator completionCoordinator = mock(CompletionCoordinator.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingStateStore routingStateStore = mock(RoutingStateStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionCoordinator,
				identityService,
				routingStateStore,
				eventManager
		);
		TestIdentity identity = new TestIdentity(UUID.randomUUID(), "PlayerOne");
		when(pendingStore.peek(identity.getUniqueId())).thenReturn(Optional.of(mock(me.whereareiam.identica.model.pipeline.completion.CompletionPendingState.class)));
		when(routingStateStore.peek(identity.getUniqueId())).thenReturn(Optional.of(
				new RoutingTarget(RoutingTargetType.COMPLETED, "lobby", "cracked", null)
		));

		lifecycle.onIdentityAttached(new IdentityAttachedEvent(identity));

		verify(completionCoordinator, never()).consumeAndExecute(identity);
	}

	@Test
	void routingTargetReachedExecutesPendingCompletionForCompletedTarget() {
		CompletionPendingStore pendingStore = mock(CompletionPendingStore.class);
		CompletionCoordinator completionCoordinator = mock(CompletionCoordinator.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingStateStore routingStateStore = mock(RoutingStateStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionCoordinator,
				identityService,
				routingStateStore,
				eventManager
		);
		TestIdentity identity = new TestIdentity(UUID.randomUUID(), "PlayerOne");
		when(identityService.find(identity.getUniqueId())).thenReturn(Optional.of(identity));

		lifecycle.onRoutingTargetReached(new RoutingTargetReachedEvent(
				identity.getUniqueId(),
				new RoutingTarget(RoutingTargetType.COMPLETED, "lobby", "cracked", null),
				"lobby"
		));

		verify(completionCoordinator).consumeAndExecute(identity);
	}

	private static final class TestIdentity extends Identity {
		private TestIdentity(UUID uniqueId, String username) {
			super(uniqueId, username);
		}

		@Override
		public void sendMessage(@NonNull Component message) {
		}

		@Override
		public void sendTitle(@NonNull Title title) {
		}

		@Override
		public boolean hasPermission(@NonNull String permission) {
			return true;
		}

		@Override
		public @NonNull Locale getLocale() {
			return Locale.ENGLISH;
		}

		@Override
		public @NonNull Audience getAudience() {
			return Audience.empty();
		}

		@Override
		public void disconnect(@NonNull Component reason) {
		}
	}
}
