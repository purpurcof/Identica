package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.common.replication.ReplicationTestFixtures;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.provider.Providers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Provider-Attempt Store")
class DefaultProviderAttemptStoreTest {
	@DisplayName("Provider attempts are keyed by IP as well as username")
	@Test
	void attemptKeyIncludesIp() {
		DefaultProviderAttemptStore store = new DefaultProviderAttemptStore(
				new DefaultReplicationSystem(new ReplicationTestFixtures.TestReplicationAdapter()),
				this::replication,
				this::settings
		);

		store.markAttempt("premium", "verify", "SharedName", "1.1.1.1");

		assertTrue(store.hasAttempt("premium", "verify", "SharedName", "1.1.1.1"));
	}

	@DisplayName("Provider attempts do not leak across different IP addresses")
	@Test
	void attemptLookupRequiresMatchingIp() {
		DefaultProviderAttemptStore store = new DefaultProviderAttemptStore(
				new DefaultReplicationSystem(new ReplicationTestFixtures.TestReplicationAdapter()),
				this::replication,
				this::settings
		);

		store.markAttempt("premium", "verify", "SharedName", "1.1.1.1");

		assertFalse(
				store.hasAttempt("premium", "verify", "SharedName", "2.2.2.2"),
				"provider attempts should be isolated per IP for the same username"
		);
	}

	private Providers settings() {
		Providers settings = new Providers();
		Providers.Behavior behavior = new Providers.Behavior();
		behavior.setAttemptTtl(Duration.ofMinutes(1));
		settings.setBehavior(behavior);
		return settings;
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		cache.setAttempts("attempts");
		replication.setCache(cache);
		return replication;
	}
}
