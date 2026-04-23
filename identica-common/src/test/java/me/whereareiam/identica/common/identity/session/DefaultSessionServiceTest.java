package me.whereareiam.identica.common.identity.session;

import me.whereareiam.identica.common.event.EventController;
import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.common.replication.ReplicationTestFixtures;
import me.whereareiam.identica.common.replication.event.DefaultReplicatedEventRegistry;
import me.whereareiam.identica.common.replication.event.ReplicatedEventBridge;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.session.SessionClosedEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.SessionCloseRequest;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Session Service")
class DefaultSessionServiceTest {
	@DisplayName("Closing a session publishes the request and emits a session-closed event")
	@Test
	void closePublishesRequestAndEmitsEventWithMessage() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		DefaultReplicationSystem replicationSystem = new DefaultReplicationSystem(adapter);
		EventController eventController = new EventController();
		SessionCloseCapture capture = new SessionCloseCapture();
		replicatedEventBridge("alpha", replicationSystem, eventController);
		eventController.register(capture);

		DefaultSessionService service = sessionService("alpha", replicationSystem, eventController);
		UUID uniqueId = UUID.randomUUID();
		service.open(session(uniqueId)).join();

		service.close(SessionCloseRequest.builder()
				.requestId(UUID.randomUUID())
				.uniqueId(uniqueId)
				.disconnectMessage("closed")
				.build()).join();

		assertEquals(1, adapter.publishCalls);
		assertEquals("identica:events", adapter.lastPublishChannel);
		assertEquals(uniqueId, capture.event.getUniqueId());
		assertEquals("closed", capture.event.getRequest().getDisconnectMessage());
		assertTrue(service.findByUniqueId(uniqueId).join().isEmpty());
	}

	@DisplayName("Remote session-close events are applied without being republished")
	@Test
	void remoteCloseIsAppliedWithoutRepublishing() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		DefaultReplicationSystem replicationSystem = new DefaultReplicationSystem(adapter);
		EventController firstEvents = new EventController();
		EventController secondEvents = new EventController();
		SessionCloseCapture secondCapture = new SessionCloseCapture();
		replicatedEventBridge("alpha", replicationSystem, firstEvents);
		replicatedEventBridge("beta", replicationSystem, secondEvents);
		secondEvents.register(secondCapture);

		DefaultSessionService first = sessionService("alpha", replicationSystem, firstEvents);
		DefaultSessionService second = sessionService("beta", replicationSystem, secondEvents);

		UUID uniqueId = UUID.randomUUID();
		first.open(session(uniqueId)).join();
		second.open(session(uniqueId)).join();

		first.close(SessionCloseRequest.builder()
				.requestId(UUID.randomUUID())
				.uniqueId(uniqueId)
				.disconnectMessage("remote close")
				.build()).join();
		adapter.emit(adapter.lastPublishPayload);

		assertEquals(1, adapter.publishCalls);
		assertTrue(second.findByUniqueId(uniqueId).join().isEmpty());
		assertEquals("remote close", secondCapture.event.getRequest().getDisconnectMessage());
	}

	@DisplayName("A node ignores the session-close events that it published itself")
	@Test
	void ownPublishedCloseIsIgnoredWhenReceivedBack() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		DefaultReplicationSystem replicationSystem = new DefaultReplicationSystem(adapter);
		EventController eventController = new EventController();
		SessionCloseCapture capture = new SessionCloseCapture();
		replicatedEventBridge("alpha", replicationSystem, eventController);
		eventController.register(capture);

		DefaultSessionService service = sessionService("alpha", replicationSystem, eventController);
		UUID uniqueId = UUID.randomUUID();
		service.open(session(uniqueId)).join();

		service.close(SessionCloseRequest.builder()
				.requestId(UUID.randomUUID())
				.uniqueId(uniqueId)
				.disconnectMessage("closed")
				.build()).join();
		SessionClosedEvent original = capture.event;
		capture.event = null;

		adapter.emit(adapter.lastPublishPayload);

		assertEquals(1, adapter.publishCalls);
		assertNull(capture.event);
		assertEquals(uniqueId, original.getUniqueId());
	}

	private DefaultSessionService sessionService(
			String serverId,
			DefaultReplicationSystem replicationSystem,
			EventController eventController
	) {
		return new DefaultSessionService(
				this::settings,
				eventController,
				() -> replication(serverId),
				replicationSystem
		);
	}

	private void replicatedEventBridge(
			String serverId,
			DefaultReplicationSystem replicationSystem,
			EventController eventController
	) {
		new ReplicatedEventBridge(
				eventController,
				replicationSystem,
				() -> replication(serverId),
				new DefaultReplicatedEventRegistry()
		);
	}

	private Session session(UUID uniqueId) {
		return Session.builder()
				.uniqueId(uniqueId)
				.providerId("provider")
				.providerSubject(uniqueId.toString())
				.originalUsername("Player")
				.build();
	}

	private Settings settings() {
		Settings settings = new Settings();
		Settings.Connection connection = new Settings.Connection();
		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setDefaultTtl(Duration.ofMinutes(5));
		sessions.setConcurrencyPolicy(SessionConcurrencyPolicy.REPLACE_EXISTING);
		connection.setSessions(sessions);
		settings.setConnection(connection);
		return settings;
	}

	private Replication replication(String serverId) {
		Replication replication = new Replication();
		replication.setEnabled(true);
		replication.setServerId(serverId);

		Replication.Cache cache = new Replication.Cache();
		Replication.Sessions sessions = new Replication.Sessions();
		sessions.setUser("sessions:user");
		sessions.setSession("sessions:session");
		sessions.setSubject("sessions:subject");
		cache.setSessions(sessions);
		replication.setCache(cache);

		Replication.Redis redis = new Replication.Redis();
		Replication.Channels channels = new Replication.Channels();
		channels.setSessions("identica:sessions");
		channels.setEvents("identica:events");
		redis.setChannels(channels);
		replication.setRedis(redis);

		return replication;
	}

	private static final class SessionCloseCapture implements EventListener {
		private SessionClosedEvent event;

		@IdenticEvent
		public void onSessionClosed(SessionClosedEvent event) {
			this.event = event;
		}
	}
}
