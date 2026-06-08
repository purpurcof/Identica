package me.whereareiam.identica.common.identity.session;

import me.whereareiam.identica.common.identity.session.recognition.DefaultSessionRecognitionStore;
import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.common.replication.ReplicationTestFixtures;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.session.SessionRecognitionSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Default Session-Recognition Store")
class DefaultSessionRecognitionStoreTest {
	@DisplayName("Recognition snapshots are keyed by provider id and subject")
	@Test
	void snapshotsUseProviderIdAndSubjectKey() {
		DefaultSessionRecognitionStore store = new DefaultSessionRecognitionStore(
				this::settings,
				this::replication,
				new DefaultReplicationSystem(new ReplicationTestFixtures.TestReplicationAdapter())
		);

		store.save(SessionRecognitionSnapshot.builder()
				.providerId("premium")
				.providerSubject("subject-1")
				.providerUsername("PlayerOne")
				.lastIp("127.0.0.1")
				.capturedAt(System.currentTimeMillis())
				.build());

		SessionRecognitionSnapshot found = store.find("premium", "subject-1").orElse(null);
		assertNotNull(found);
		assertEquals("PlayerOne", found.getProviderUsername());
		assertEquals("127.0.0.1", found.getLastIp());
	}

	@DisplayName("Recognition snapshots do not leak across subjects")
	@Test
	void snapshotsRequireMatchingSubject() {
		DefaultSessionRecognitionStore store = new DefaultSessionRecognitionStore(
				this::settings,
				this::replication,
				new DefaultReplicationSystem(new ReplicationTestFixtures.TestReplicationAdapter())
		);

		store.save(SessionRecognitionSnapshot.builder()
				.providerId("premium")
				.providerSubject("subject-1")
				.providerUsername("PlayerOne")
				.capturedAt(System.currentTimeMillis())
				.build());

		assertTrue(store.find("premium", "subject-2").isEmpty());
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		Replication.Sessions sessions = new Replication.Sessions();
		Replication.Sessions.Recognition recognition = new Replication.Sessions.Recognition();
		recognition.setSnapshot("session-recognition:snapshot");
		sessions.setRecognition(recognition);
		cache.setSessions(sessions);
		replication.setCache(cache);
		return replication;
	}

	private Settings settings() {
		Settings settings = new Settings();
		Settings.Sessions sessions = new Settings.Sessions();
		Settings.Sessions.Recognition recognition = new Settings.Sessions.Recognition();
		recognition.setValidity(java.time.Duration.ofHours(12));
		sessions.setRecognition(recognition);
		settings.setSessions(sessions);
		return settings;
	}
}
