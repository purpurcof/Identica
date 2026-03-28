package me.whereareiam.identica.common.handshake;

import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.common.replication.ReplicationTestFixtures;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Replication;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DefaultHandshakeStoreTest {
	@Test
	void consumeUsesMatchingUsernameAndIp() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		adapter.available = false;
		DefaultHandshakeStore store = new DefaultHandshakeStore(
				new DefaultReplicationSystem(adapter),
				this::replication,
				mock(EventManager.class)
		);
		HandshakeInstruction instruction = HandshakeInstruction.create(
				new ConnectionIdentity("PlayerOne", "1.1.1.1"),
				Duration.ofMinutes(1).toMillis()
		);

		store.putInstruction(instruction);

		Optional<HandshakeInstruction> resolved = store.consumeInstruction("PlayerOne", "1.1.1.1");
		assertTrue(resolved.isPresent());
		assertEquals("PlayerOne", resolved.get().getIdentity().getUsername());
	}

	@Test
	void consumeRequiresMatchingIp() {
		ReplicationTestFixtures.TestReplicationAdapter adapter = new ReplicationTestFixtures.TestReplicationAdapter();
		adapter.available = false;
		DefaultHandshakeStore store = new DefaultHandshakeStore(
				new DefaultReplicationSystem(adapter),
				this::replication,
				mock(EventManager.class)
		);
		HandshakeInstruction instruction = HandshakeInstruction.create(
				new ConnectionIdentity("PlayerOne", "1.1.1.1"),
				Duration.ofMinutes(1).toMillis()
		);

		store.putInstruction(instruction);

		Optional<HandshakeInstruction> resolved = store.consumeInstruction("PlayerOne", "2.2.2.2");
		assertFalse(resolved.isPresent(), "handshake instructions should not bleed across IPs for the same username");
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		cache.setInstructions("instructions");
		replication.setCache(cache);
		return replication;
	}
}
