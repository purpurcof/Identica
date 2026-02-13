package me.whereareiam.identica.common.replication;

import me.whereareiam.identica.common.replication.cache.InMemoryLocalCache;
import me.whereareiam.identica.model.replication.ReplicationPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLocalCacheTest {
	private InMemoryLocalCache<String> cache;

	@BeforeEach
	void setUp() {
		cache = new InMemoryLocalCache<>();
	}

	@Test
	void putGetReturnsValueWhenTtlPositive() {
		cache.put("key", "value", 500).join();
		assertEquals(Optional.of("value"), cache.get("key").join());
	}

	@Test
	void putWithZeroTtlRemovesEntry() {
		cache.put("key", "value", 500).join();
		cache.put("key", "value", 0).join();
		assertEquals(Optional.empty(), cache.get("key").join());
	}

	@Test
	void invalidateRemovesEntry() {
		cache.put("key", "value", 500).join();
		cache.invalidate("key").join();
		assertEquals(Optional.empty(), cache.get("key").join());
	}

	@Test
	void consumeReturnsAndRemovesEntry() {
		cache.put("key", "value", 500).join();
		assertEquals(Optional.of("value"), cache.consume("key").join());
		assertEquals(Optional.empty(), cache.get("key").join());
	}

	@Test
	void expiredEntryIsRemoved() {
		cache.put("key", "value", 30).join();
		sleep(50);
		assertEquals(Optional.empty(), cache.get("key").join());
	}

	@Test
	void listKeysSortsAndPaginates() {
		cache.put("b", "value", 500).join();
		cache.put("a", "value", 500).join();
		cache.put("c", "value", 500).join();

		ReplicationPage page1 = cache.listKeys(1, 2).join();
		ReplicationPage page2 = cache.listKeys(2, 2).join();

		assertEquals(List.of("a", "b"), page1.getEntries());
		assertEquals(3, page1.getTotal());
		assertEquals(List.of("c"), page2.getEntries());
		assertEquals(3, page2.getTotal());
	}

	@Test
	void listKeysCleansExpiredEntries() {
		cache.put("alive", "value", 500).join();
		cache.put("expired", "value", 20).join();
		sleep(40);

		ReplicationPage page = cache.listKeys(1, 10).join();
		assertTrue(page.getEntries().contains("alive"));
		assertEquals(1, page.getTotal());
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
