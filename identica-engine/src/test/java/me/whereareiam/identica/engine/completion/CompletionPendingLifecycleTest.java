package me.whereareiam.identica.engine.completion;

import me.whereareiam.identica.engine.pipeline.completion.CompletionPendingLifecycle;
import me.whereareiam.identica.engine.pipeline.completion.CompletionPipeline;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.service.DeliveryService;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@DisplayName("Completion Pending Lifecycle")
class CompletionPendingLifecycleTest {
	@DisplayName("Opening a session stores pending completion state")
	@Test
	void sessionOpenedStoresPendingCompletion() {
		DeliveryService deliveryService = mock(DeliveryService.class);
		CompletionPipeline completionPipeline = mock(CompletionPipeline.class);
		EventManager eventManager = mock(EventManager.class);
		Settings settings = new Settings();
		settings.setConnection(new Settings.Connection());
		settings.getConnection().getRouting().getDefaults().getComplete().setTarget("limbo");
		CompletionPendingLifecycle lifecycle = new CompletionPendingLifecycle(
				deliveryService,
				completionPipeline,
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
				PipelineType.AUTHENTICATION,
				session,
				true
		));

		verify(deliveryService).queue(argThat((DeliveryRequest request) ->
				request != null
						&& request.getPayload().getCompletion() != null
						&& request.getPayload().getCompletion().isAuthenticationRecognized()
						&& "limbo".equals(request.getRequiredServer())
		));
		verify(completionPipeline, never()).complete(any());
	}
}
