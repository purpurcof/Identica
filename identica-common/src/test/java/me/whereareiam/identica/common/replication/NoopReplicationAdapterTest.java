package me.whereareiam.identica.common.replication;

import me.whereareiam.identica.model.replication.ReplicationPage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NoopReplicationAdapterTest {
	@Test
	void noopAdapterReturnsEmptyAndUnavailable() {
		NoopReplicationAdapter adapter = new NoopReplicationAdapter();

		assertFalse(adapter.isAvailable());
		assertEquals(Optional.empty(), adapter.get("ns", "key").join());
		assertEquals(Optional.empty(), adapter.consume("ns", "key").join());
		assertNull(adapter.put("ns", "key", new byte[]{1}, 5).join());
		assertNull(adapter.invalidate("ns", "key").join());
		assertNull(adapter.publish("channel", new byte[]{1}).join());

		ReplicationPage page = adapter.listKeys("ns", 0, 0).join();
		assertEquals(1, page.getPage());
		assertEquals(1, page.getPageSize());
		assertEquals(0, page.getTotal());
	}
}
