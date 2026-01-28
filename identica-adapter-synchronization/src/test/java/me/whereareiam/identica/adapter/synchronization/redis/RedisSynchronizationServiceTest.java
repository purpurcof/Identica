package me.whereareiam.identica.adapter.synchronization.redis;

import com.redis.testcontainers.RedisContainer;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.adapter.synchronization.RedisSynchronizationService;
import me.whereareiam.identica.adapter.synchronization.provider.JedisPoolProvider;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.registry.Registry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
class RedisSynchronizationServiceTest {
	@Mock
	private Registry<Reloadable> reloadableRegistry;

	@Container
	private static final RedisContainer REDIS = new RedisContainer("redis:8.4");

	@Test
	void putGetAndInvalidate() throws Exception {
		Replication replication = new Replication();
		replication.setEnabled(true);
		Replication.Redis redis = new Replication.Redis();
		redis.setHost(REDIS.getRedisHost());
		redis.setPort(REDIS.getRedisPort());
		redis.setPassword("");
		redis.setTimeout(2000);
		redis.setSsl(false);
		replication.setRedis(redis);

		JedisPoolProvider poolProvider = new JedisPoolProvider(() -> replication, reloadableRegistry);
		RedisSynchronizationService backend = new RedisSynchronizationService(poolProvider, () -> replication);

		byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
		backend.put("premium-profile", "Steve", payload, 5000).get(2, TimeUnit.SECONDS);

		Optional<byte[]> fetched = backend.get("premium-profile", "Steve").get(2, TimeUnit.SECONDS);
		assertTrue(fetched.isPresent());
		assertArrayEquals(payload, fetched.get());

		backend.invalidate("premium-profile", "Steve").get(2, TimeUnit.SECONDS);
		Optional<byte[]> missing = backend.get("premium-profile", "Steve").get(2, TimeUnit.SECONDS);
		assertTrue(missing.isEmpty());
	}
}
