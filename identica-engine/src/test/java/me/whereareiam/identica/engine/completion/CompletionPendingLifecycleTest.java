package me.whereareiam.identica.engine.completion;

import me.whereareiam.identica.engine.pipeline.completion.lifecycle.CompletionPendingLifecycle;
import me.whereareiam.identica.engine.pipeline.completion.runtime.CompletionPipeline;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.delivery.DeliveryCheckpointReachedEvent;
import me.whereareiam.identica.event.routing.completion.CompletionRoutingReachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Routing;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.model.routing.RoutingEndpoint;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptState;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.service.PlatformDeliveryAdapter;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.routing.reason.RoutingReason;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@DisplayName("Completion Pending Lifecycle")
class CompletionPendingLifecycleTest {
	@DisplayName("Opening a session stores pending completion state for the scenario completion route")
	@Test
	void sessionOpenedStoresPendingCompletion() {
		DeliveryService deliveryService = mock(DeliveryService.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		PlatformDeliveryAdapter platformDeliveryAdapter = mock(PlatformDeliveryAdapter.class);
		EventManager eventManager = mock(EventManager.class);
		Routing settings = new Routing();
		settings.getDefaults().getComplete().setTarget("limbo");
		Routing.Targets migrationTargets = new Routing.Targets();
		migrationTargets.getComplete().setTarget("migration-limbo");
		settings.getScenarios().put("migration", migrationTargets);
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				deliveryService,
				completionPipeline,
				identityService,
				platformDeliveryAdapter,
				() -> settings,
				eventManager
		);
		UUID connectionUniqueId = UUID.randomUUID();
		Session session = Session.builder()
				.uniqueId(UUID.randomUUID())
				.providerId("credential")
				.providerSubject("player-one")
				.build();

		lifecycle.onSessionOpened(new SessionOpenedEvent(
				connectionUniqueId,
				PipelineType.MIGRATION,
				session
		));

		verify(deliveryService).queue(argThat((DeliveryRequest request) ->
				request != null
						&& request.getPayload().getCompletion() != null
						&& request.getCheckpoint() == DeliveryCheckpoint.PLATFORM_READY_INITIAL
						&& "migration-limbo".equals(request.getRequiredServer())
		));
		verify(completionPipeline, never()).complete(any());
	}

	@DisplayName("Authentication completion keeps using the initial ready checkpoint")
	@Test
	void authenticationCompletionUsesInitialReadyCheckpoint() {
		DeliveryService deliveryService = mock(DeliveryService.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		PlatformDeliveryAdapter platformDeliveryAdapter = mock(PlatformDeliveryAdapter.class);
		EventManager eventManager = mock(EventManager.class);
		Routing settings = new Routing();
		settings.getDefaults().getComplete().setTarget("survival");
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				deliveryService,
				completionPipeline,
				identityService,
				platformDeliveryAdapter,
				() -> settings,
				eventManager
		);
		UUID connectionUniqueId = UUID.randomUUID();
		Session session = Session.builder()
				.uniqueId(UUID.randomUUID())
				.providerId("premium")
				.providerSubject("player-one")
				.build();

		lifecycle.onSessionOpened(new SessionOpenedEvent(
				connectionUniqueId,
				PipelineType.AUTHENTICATION,
				session
		));

		verify(deliveryService).queue(argThat((DeliveryRequest request) ->
				request != null
						&& request.getCheckpoint() == DeliveryCheckpoint.PLATFORM_READY_INITIAL
						&& "survival".equals(request.getRequiredServer())
		));
		verify(completionPipeline, never()).complete(any());
	}

	@DisplayName("Completion routing reached rearms the platform ready checkpoint")
	@Test
	void completionRoutingReachedRearmsPlatformReady() {
		DeliveryService deliveryService = mock(DeliveryService.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		PlatformDeliveryAdapter platformDeliveryAdapter = mock(PlatformDeliveryAdapter.class);
		EventManager eventManager = mock(EventManager.class);
		Routing settings = new Routing();
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				deliveryService,
				completionPipeline,
				identityService,
				platformDeliveryAdapter,
				() -> settings,
				eventManager
		);
		UUID connectionUniqueId = UUID.randomUUID();
		UUID accountUniqueId = UUID.randomUUID();
		TestIdentity identity = new TestIdentity(connectionUniqueId, accountUniqueId, "PlayerOne");
		RoutingIntent intent = new RoutingIntent(
				UUID.randomUUID(),
				connectionUniqueId,
				new RoutingEndpoint("survival"),
				RoutingReason.COMPLETION,
				RoutingAttemptPolicy.defaultCompletion(),
				new RoutingAttemptState(),
				PipelineType.MIGRATION,
				null,
				"credential",
				null,
				System.currentTimeMillis()
		);

		when(identityService.findByConnectionUniqueId(connectionUniqueId)).thenReturn(Optional.of(identity));

		lifecycle.onCompletionRoutingReached(new CompletionRoutingReachedEvent(intent, "survival"));

		verify(platformDeliveryAdapter).armInitialReady(identity, "survival");
		verify(completionPipeline, never()).complete(any(), any());
		verify(deliveryService, never()).acknowledge(any(), any());
	}

	@DisplayName("Delivery checkpoint dispatches queued completion")
	@Test
	void deliveryCheckpointDispatchesQueuedCompletion() {
		DeliveryService deliveryService = mock(DeliveryService.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		IdentityService identityService = mock(IdentityService.class);
		PlatformDeliveryAdapter platformDeliveryAdapter = mock(PlatformDeliveryAdapter.class);
		EventManager eventManager = mock(EventManager.class);
		Routing settings = new Routing();
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				deliveryService,
				completionPipeline,
				identityService,
				platformDeliveryAdapter,
				() -> settings,
				eventManager
		);
		UUID connectionUniqueId = UUID.randomUUID();
		UUID accountUniqueId = UUID.randomUUID();
		TestIdentity identity = new TestIdentity(connectionUniqueId, accountUniqueId, "PlayerOne");
		UUID deliveryId = UUID.randomUUID();
		DeliveryRequest request = DeliveryRequest.builder()
				.id(deliveryId)
				.source(me.whereareiam.identica.type.messaging.DeliverySource.COMPLETION)
				.target(me.whereareiam.identica.model.delivery.DeliveryTarget.builder()
						.connectionUniqueId(connectionUniqueId)
						.accountUniqueId(accountUniqueId)
						.build())
				.payload(me.whereareiam.identica.model.delivery.DeliveryPayload.builder()
						.completion(me.whereareiam.identica.model.delivery.DeliveryPayload.CompletionPayload.builder()
								.connectionUniqueId(connectionUniqueId)
								.accountUniqueId(accountUniqueId)
								.pipelineType(PipelineType.MIGRATION)
								.build())
						.build())
				.checkpoint(DeliveryCheckpoint.PLATFORM_READY_INITIAL)
				.semantics(me.whereareiam.identica.type.messaging.DeliverySemantics.ONCE)
				.requiredServer("survival")
				.createdAt(System.currentTimeMillis())
				.updatedAt(System.currentTimeMillis())
				.build();

		when(deliveryService.dispatch(argThat(context ->
				context.getCheckpoint() == DeliveryCheckpoint.PLATFORM_READY_INITIAL
						&& context.getIdentity() == identity
						&& "survival".equals(context.getCurrentServer())
		))).thenReturn(List.of(request));

		lifecycle.onDeliveryCheckpointReached(new DeliveryCheckpointReachedEvent(
				DeliveryCheckpoint.PLATFORM_READY_INITIAL,
				identity,
				"survival"
		));

		verify(completionPipeline).complete(eq(identity), argThat((CompletionPendingState pendingState) ->
				pendingState != null
						&& pendingState.getPipelineType() == PipelineType.MIGRATION
						&& connectionUniqueId.equals(pendingState.getConnectionUniqueId())
						&& accountUniqueId.equals(pendingState.getAccountUniqueId())
		));
		verify(deliveryService).acknowledge(deliveryId, "completion-dispatched");
	}

	private static final class TestIdentity extends Identity {
		private TestIdentity(UUID connectionUniqueId, UUID accountUniqueId, String username) {
			super(connectionUniqueId, accountUniqueId, username, "127.0.0.1");
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
