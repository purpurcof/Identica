package me.whereareiam.identica.adapter.replication;

import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.adapter.replication.provider.JedisPoolProvider;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JedisPoolProviderTest {
	@Test
	void disabledReplicationThrowsAndReturnsEmptyOptional() {
		Replication replication = new Replication();
		replication.setEnabled(false);

		Registry<Reloadable> registry = RedisTestFixtures.reloadableRegistry();
		JedisPoolProvider provider = new JedisPoolProvider(() -> replication, registry);

		assertThrows(RuntimeException.class, provider::get);
		assertTrue(provider.getOptional().isEmpty());
		provider.reload();
		assertTrue(provider.getOptional().isEmpty());
	}

	@Test
	void unreachableRedisReturnsEmptyOptional() {
		Replication replication = RedisTestFixtures.enabledReplication("127.0.0.1", 6399);
		replication.setEnabled(false);
		replication.getRedis().setTimeout(100);

		Registry<Reloadable> registry = RedisTestFixtures.reloadableRegistry();
		JedisPoolProvider provider = new JedisPoolProvider(() -> replication, registry);

		replication.setEnabled(true);
		assertTrue(provider.getOptional().isEmpty());
	}
}
