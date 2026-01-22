package me.whereareiam.identica.adapter.synchronization;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.synchronization.provider.JedisPoolProvider;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.synchronization.SynchronizationService;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@SuppressWarnings("resource")
public class RedisSynchronizationService implements SynchronizationService {
	private static final String KV_PREFIX = "identica";

	private final JedisPoolProvider poolProvider;
	private final Provider<Settings> settingsProvider;

	@Override
	public boolean isAvailable() {
		Settings.Synchronization sync = settingsProvider.get().getSynchronization();
		return sync != null && sync.isEnabled() && sync.getRedis() != null;
	}

	@Override
	public CompletableFuture<Optional<byte[]>> get(String namespace, String key) {
		if (!isAvailable()) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		return CompletableFuture.supplyAsync(() -> {
			JedisPool pool = poolProvider.getOptional().orElse(null);
			if (pool == null) {
				return Optional.empty();
			}
			byte[] rawKey = buildKey(namespace, key);
			try (Jedis jedis = pool.getResource()) {
				return Optional.ofNullable(jedis.get(rawKey));
			} catch (Exception ignored) {
				return Optional.empty();
			}
		});
	}

	@Override
	public CompletableFuture<Void> put(String namespace, String key, byte[] value, long ttlMs) {
		if (!isAvailable()) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.runAsync(() -> {
			JedisPool pool = poolProvider.getOptional().orElse(null);
			if (pool == null) {
				return;
			}
			byte[] rawKey = buildKey(namespace, key);
			try (Jedis jedis = pool.getResource()) {
				if (ttlMs > 0) {
					jedis.psetex(rawKey, ttlMs, value);
				} else {
					jedis.set(rawKey, value);
				}
			} catch (Exception ignored) {
				// ignore failures for cache
			}
		});
	}

	@Override
	public CompletableFuture<Void> invalidate(String namespace, String key) {
		if (!isAvailable()) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.runAsync(() -> {
			JedisPool pool = poolProvider.getOptional().orElse(null);
			if (pool == null) {
				return;
			}
			byte[] rawKey = buildKey(namespace, key);
			try (Jedis jedis = pool.getResource()) {
				jedis.del(rawKey);
			} catch (Exception ignored) {
				// ignore failures for sync
			}
		});
	}

	@Override
	public CompletableFuture<Void> publish(String channel, byte[] payload) {
		if (!isAvailable() || channel == null || channel.isBlank() || payload == null) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.runAsync(() -> {
			JedisPool pool = poolProvider.getOptional().orElse(null);
			if (pool == null) {
				return;
			}
			byte[] rawChannel = channel.getBytes(StandardCharsets.UTF_8);
			try (Jedis jedis = pool.getResource()) {
				jedis.publish(rawChannel, payload);
			} catch (Exception ignored) {
				// ignore failures for sync
			}
		});
	}

	@Override
	public void subscribe(String channel, java.util.function.Consumer<byte[]> handler) {
		if (!isAvailable() || channel == null || channel.isBlank() || handler == null) {
			return;
		}
		JedisPool pool = poolProvider.getOptional().orElse(null);
		if (pool == null) {
			return;
		}
		byte[] rawChannel = channel.getBytes(StandardCharsets.UTF_8);
		Thread thread = new Thread(() -> {
			try (Jedis jedis = pool.getResource()) {
				jedis.subscribe(new BinaryJedisPubSub() {
					@Override
					public void onMessage(byte[] channel, byte[] message) {
						handler.accept(message);
					}
				}, rawChannel);
			} catch (Exception ignored) {
				// ignore failures for sync
			}
		}, "identica-redis-sub-" + channel);
		thread.setDaemon(true);
		thread.start();
	}

	private byte[] buildKey(String namespace, String key) {
		StringBuilder builder = new StringBuilder(KV_PREFIX);
		if (namespace != null && !namespace.isBlank()) {
			builder.append(':').append(namespace);
		}
		if (key != null && !key.isBlank()) {
			builder.append(':').append(key);
		}
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}
}
