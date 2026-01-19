package me.whereareiam.identica.adapter.synchronization.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.registry.Registry;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

@Singleton
public class JedisPoolProvider implements Provider<JedisPool>, Reloadable {
	private final Provider<Settings> configProvider;
	private JedisPool jedisPool;

	@Inject
	public JedisPoolProvider(
			Provider<Settings> configProvider,
			Registry<Reloadable> reloadableRegistry
	) {
		this.configProvider = configProvider;

		reloadableRegistry.register(this);
		get();
	}

	@Override
	public JedisPool get() {
		if (jedisPool != null)
			return jedisPool;

		Settings.Synchronization sync = configProvider.get().getSynchronization();
		if (sync == null || sync.getRedis() == null) {
			throw new RuntimeException("Synchronization settings are missing");
		}
		Settings.Synchronization.Redis redis = sync.getRedis();

		try {
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			jedisPool = new JedisPool(
					poolConfig,
					redis.getHost(),
					redis.getPort(),
					(int) redis.getTimeout(),
					redis.isSsl()
			);

			// Test connection
			jedisPool.getResource().close();

			return jedisPool;
		} catch (JedisConnectionException e) {
			throw new RuntimeException("Failed to connect to Redis", e);
		}
	}

	public synchronized void close() {
		if (jedisPool != null) {
			jedisPool.close();
			jedisPool = null;
		}
	}

	@Override
	public void reload() {
		if (jedisPool != null)
			jedisPool.close();

		jedisPool = get();
	}
}
