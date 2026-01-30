package me.whereareiam.identica.common.cache.type;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalCacheTest {
	@Test
	void respectsTtlAndInvalidate() throws Exception {
		LocalCache<String> cache = new LocalCache<>();

		cache.put("key", "value", 200).get(1, TimeUnit.SECONDS);
		Optional<String> first = cache.get("key").get(1, TimeUnit.SECONDS);
		assertEquals(Optional.of("value"), first);

		Thread.sleep(250);
		Optional<String> expired = cache.get("key").get(1, TimeUnit.SECONDS);
		assertTrue(expired.isEmpty());

		cache.put("key", "value", 500).get(1, TimeUnit.SECONDS);
		cache.invalidate("key").get(1, TimeUnit.SECONDS);
		Optional<String> invalidated = cache.get("key").get(1, TimeUnit.SECONDS);
		assertTrue(invalidated.isEmpty());
	}
}

