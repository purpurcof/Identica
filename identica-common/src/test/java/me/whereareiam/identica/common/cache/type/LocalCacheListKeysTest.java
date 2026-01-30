package me.whereareiam.identica.common.cache.type;

import me.whereareiam.identica.cache.Cache;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalCacheListKeysTest {
	@Test
	void listExcludesExpired() throws Exception {
		LocalCache<String> cache = new LocalCache<>();
		cache.put("expired", "value", 1).get(1, TimeUnit.SECONDS);
		cache.put("active", "value", TimeUnit.MINUTES.toMillis(1)).get(1, TimeUnit.SECONDS);

		TimeUnit.MILLISECONDS.sleep(5);

		Cache.Page page = cache.listKeys(1, 10).get(1, TimeUnit.SECONDS);
		assertEquals(1, page.total());
		assertEquals(List.of("active"), page.entries());
	}

	@Test
	void paginationOrdersByKey() throws Exception {
		LocalCache<String> cache = new LocalCache<>();
		cache.put("c", "value", TimeUnit.MINUTES.toMillis(1)).get(1, TimeUnit.SECONDS);
		cache.put("a", "value", TimeUnit.MINUTES.toMillis(1)).get(1, TimeUnit.SECONDS);
		cache.put("b", "value", TimeUnit.MINUTES.toMillis(1)).get(1, TimeUnit.SECONDS);

		Cache.Page page1 = cache.listKeys(1, 2).get(1, TimeUnit.SECONDS);
		Cache.Page page2 = cache.listKeys(2, 2).get(1, TimeUnit.SECONDS);

		assertEquals(List.of("a", "b"), page1.entries());
		assertEquals(List.of("c"), page2.entries());
		assertEquals(3, page1.total());
	}
}

