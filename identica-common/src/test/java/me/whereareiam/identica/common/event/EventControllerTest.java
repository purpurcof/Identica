package me.whereareiam.identica.common.event;

import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentEvent;
import me.whereareiam.identica.event.routing.intent.RoutingIntentStartedEvent;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptState;
import me.whereareiam.identica.model.routing.RoutingEndpoint;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.routing.RoutingReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Event Controller")
class EventControllerTest {
	@DisplayName("Interface-based routing listeners receive concrete routing intent events")
	@Test
	void routingIntentInterfaceListenerReceivesConcreteIntentEvent() {
		EventController eventController = new EventController();
		RoutingListener listener = new RoutingListener();
		eventController.register(listener);

		eventController.call(new RoutingIntentStartedEvent(intent()));

		assertEquals(1, listener.intentEvents.get());
	}

	private RoutingIntent intent() {
		UUID connectionId = UUID.randomUUID();
		return new RoutingIntent(
				UUID.randomUUID(),
				connectionId,
				new RoutingEndpoint("auth"),
				RoutingReason.STEP,
				RoutingAttemptPolicy.defaultStep(),
				new RoutingAttemptState(),
				PipelineType.AUTHENTICATION,
				null,
				null,
				null,
				System.currentTimeMillis()
		);
	}

	private static class RoutingListener implements EventListener {
		private final AtomicInteger intentEvents = new AtomicInteger();

		@IdenticEvent
		public void onRoutingIntent(RoutingIntentEvent event) {
			intentEvents.incrementAndGet();
		}
	}
}
