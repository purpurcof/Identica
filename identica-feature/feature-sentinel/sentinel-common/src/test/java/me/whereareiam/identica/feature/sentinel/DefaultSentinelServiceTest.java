package me.whereareiam.identica.feature.sentinel;

import me.whereareiam.identica.feature.sentinel.model.config.SentinelSettings;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.replication.cache.base.ReplicationCacheBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Default Sentinel Service")
class DefaultSentinelServiceTest {
	@DisplayName("Service reads the cache namespace from sentinel feature settings")
	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void serviceReadsCacheNamespaceFromFeatureSettings() {
		ReplicationSystem replicationSystem = mock(ReplicationSystem.class);
		ReplicationCacheBuilder cacheBuilder = mock(ReplicationCacheBuilder.class);
		ReplicatedCache cache = mock(ReplicatedCache.class);
		when(replicationSystem.cache("identica:test:sentinels")).thenReturn(cacheBuilder);
		when(cacheBuilder.replicated(any())).thenReturn(cache);

		SentinelSettings settings = new SentinelSettings();
		SentinelSettings.Replication replicationSettings = new SentinelSettings.Replication();
		replicationSettings.setState("identica:test:sentinels");
		settings.setReplication(replicationSettings);

		new DefaultSentinelService(new SentinelRegistry(), replicationSystem, () -> settings);

		verify(replicationSystem).cache("identica:test:sentinels");
	}
}
