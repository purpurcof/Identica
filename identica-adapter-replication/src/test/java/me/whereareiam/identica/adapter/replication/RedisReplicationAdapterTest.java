package me.whereareiam.identica.adapter.replication;

import me.whereareiam.identica.adapter.replication.provider.JedisPoolProvider;
import me.whereareiam.identica.model.replication.ReplicationPage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisReplicationAdapterTest {
	@Test
	void unavailableAdapterNoopsAllCalls() {
		JedisPoolProvider poolProvider = mock(JedisPoolProvider.class);
		RedisReplicationAdapter adapter = new RedisReplicationAdapter(poolProvider, RedisReplicationAdapterTest::disabledReplication);

		assertEquals(Optional.empty(), adapter.get("ns", "key").join());
		assertEquals(Optional.empty(), adapter.consume("ns", "key").join());
		assertNull(adapter.put("ns", "key", new byte[]{1}, 5).join());
		assertNull(adapter.invalidate("ns", "key").join());
		ReplicationPage page = adapter.listKeys("ns", 0, 0).join();
		assertEquals(1, page.getPage());
		assertEquals(1, page.getPageSize());
		assertEquals(0, page.getTotal());

		verifyNoInteractions(poolProvider);
	}

	@Test
	void publishWithBlankChannelOrNullPayloadNoops() {
		JedisPoolProvider poolProvider = mock(JedisPoolProvider.class);
		RedisReplicationAdapter adapter = new RedisReplicationAdapter(poolProvider, RedisReplicationAdapterTest::enabledReplication);

		adapter.publish("", new byte[] {1}).join();
		adapter.publish("channel", null).join();

		verifyNoInteractions(poolProvider);
	}

	private static me.whereareiam.identica.model.config.Replication disabledReplication() {
		me.whereareiam.identica.model.config.Replication replication = new me.whereareiam.identica.model.config.Replication();
		replication.setEnabled(false);
		return replication;
	}

	private static me.whereareiam.identica.model.config.Replication enabledReplication() {
		me.whereareiam.identica.model.config.Replication replication = new me.whereareiam.identica.model.config.Replication();
		replication.setEnabled(true);
		return replication;
	}
}
