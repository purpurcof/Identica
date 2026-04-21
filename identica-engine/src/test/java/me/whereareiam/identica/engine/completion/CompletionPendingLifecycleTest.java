package me.whereareiam.identica.engine.completion;

import me.whereareiam.identica.engine.pipeline.completion.CompletionPipeline;
import me.whereareiam.identica.engine.pipeline.completion.CompletionPendingLifecycle;
import me.whereareiam.identica.pipeline.completion.CompletionPendingStore;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentReachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptState;
import me.whereareiam.identica.model.routing.RoutingEndpoint;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.routing.RoutingIntentStore;
import me.whereareiam.identica.type.routing.RoutingReason;
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
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingIntentStore routingIntentStore = mock(RoutingIntentStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionPipeline,
				identityService,
				routingIntentStore,
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
		verify(completionPipeline, never()).consumeAndExecute(any());
	}

	@Test
	void identityAttachedConsumesPendingCompletionWithoutRoutingTarget() {
		CompletionPendingStore pendingStore = mock(CompletionPendingStore.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingIntentStore routingIntentStore = mock(RoutingIntentStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionPipeline,
				identityService,
				routingIntentStore,
				eventManager
		);
		TestIdentity identity = new TestIdentity(UUID.randomUUID(), "PlayerOne");
		when(pendingStore.peek(identity.getUniqueId())).thenReturn(Optional.of(mock(me.whereareiam.identica.model.pipeline.completion.CompletionPendingState.class)));
		when(routingIntentStore.peek(identity.getUniqueId())).thenReturn(Optional.empty());

		lifecycle.onIdentityAttached(new IdentityAttachedEvent(identity));

		verify(completionPipeline).consumeAndExecute(identity);
	}

	@Test
	void identityAttachedDefersWhileCompletedRoutingTargetExists() {
		CompletionPendingStore pendingStore = mock(CompletionPendingStore.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingIntentStore routingIntentStore = mock(RoutingIntentStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionPipeline,
				identityService,
				routingIntentStore,
				eventManager
		);
		TestIdentity identity = new TestIdentity(UUID.randomUUID(), "PlayerOne");
		when(pendingStore.peek(identity.getUniqueId())).thenReturn(Optional.of(mock(me.whereareiam.identica.model.pipeline.completion.CompletionPendingState.class)));
		when(routingIntentStore.peek(identity.getUniqueId())).thenReturn(Optional.of(completionIntent(identity.getUniqueId())));

		lifecycle.onIdentityAttached(new IdentityAttachedEvent(identity));

		verify(completionPipeline, never()).consumeAndExecute(identity);
	}

	@Test
	void routingTargetReachedExecutesPendingCompletionForCompletedTarget() {
		CompletionPendingStore pendingStore = mock(CompletionPendingStore.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		RoutingIntentStore routingIntentStore = mock(RoutingIntentStore.class);
		EventManager eventManager = mock(EventManager.class);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				pendingStore,
				completionPipeline,
				identityService,
				routingIntentStore,
				eventManager
		);
		TestIdentity identity = new TestIdentity(UUID.randomUUID(), "PlayerOne");
		when(identityService.find(identity.getUniqueId())).thenReturn(Optional.of(identity));

		lifecycle.onRoutingIntentReached(new RoutingIntentReachedEvent(completionIntent(identity.getUniqueId()), "lobby"));

		verify(completionPipeline).consumeAndExecute(identity);
	}

	private static RoutingIntent completionIntent(UUID connectionUniqueId) {
		return new RoutingIntent(
				UUID.randomUUID(),
				connectionUniqueId,
				new RoutingEndpoint("lobby"),
				RoutingReason.COMPLETION,
				RoutingAttemptPolicy.defaultCompletion(),
				new RoutingAttemptState(),
				PipelineType.AUTHENTICATION,
				null,
				"cracked",
				null,
				System.currentTimeMillis()
		);
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
