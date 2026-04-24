package me.whereareiam.identica.common.routing;

import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptReport;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptState;
import me.whereareiam.identica.model.routing.RoutingEndpoint;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;
import me.whereareiam.identica.type.routing.RoutingIntentStatus;
import me.whereareiam.identica.type.routing.RoutingReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Routing-Intent Store")
class DefaultRoutingIntentStoreTest {
	@DisplayName("Replacing an intent clears the previous attempt state")
	@Test
	void replacingIntentResetsAttemptState() {
		DefaultRoutingIntentStore store = new DefaultRoutingIntentStore();
		UUID connectionId = UUID.randomUUID();
		RoutingIntent first = intent(connectionId, "auth");
		store.put(first);
		store.recordAttempt(new RoutingAttemptReport(connectionId, RoutingAttemptTrigger.INITIAL_SERVER, true, "auth", null));

		RoutingIntent replacement = intent(connectionId, "lobby");
		store.put(replacement);

		RoutingIntent current = store.peek(connectionId).orElseThrow();
		assertEquals("lobby", current.getEndpoint().getServer());
		assertEquals(0, current.getAttemptState().getAttempts());
	}

	@DisplayName("Marks an intent as reached when the server matches")
	@Test
	void markReachedUpdatesStatusForMatchingServer() {
		DefaultRoutingIntentStore store = new DefaultRoutingIntentStore();
		UUID connectionId = UUID.randomUUID();
		store.put(intent(connectionId, "lobby"));

		assertTrue(store.markReached(connectionId, "lobby").isPresent());
		assertEquals(RoutingIntentStatus.REACHED, store.peek(connectionId).orElseThrow().getStatus());
	}

	@DisplayName("Leaves the intent pending when a different server is reported")
	@Test
	void markReachedIgnoresDifferentServer() {
		DefaultRoutingIntentStore store = new DefaultRoutingIntentStore();
		UUID connectionId = UUID.randomUUID();
		store.put(intent(connectionId, "lobby"));

		assertTrue(store.markReached(connectionId, "auth").isEmpty());
		assertEquals(RoutingIntentStatus.PENDING, store.peek(connectionId).orElseThrow().getStatus());
	}

	@DisplayName("Clearing an intent removes it from the store")
	@Test
	void clearRemovesIntent() {
		DefaultRoutingIntentStore store = new DefaultRoutingIntentStore();
		UUID connectionId = UUID.randomUUID();
		store.put(intent(connectionId, "auth"));

		assertTrue(store.clear(connectionId));
		assertFalse(store.peek(connectionId).isPresent());
	}

	private RoutingIntent intent(UUID connectionId, String server) {
		return new RoutingIntent(
				UUID.randomUUID(),
				connectionId,
				new RoutingEndpoint(server),
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
}
