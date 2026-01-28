package me.whereareiam.identica.adapter.synchronization;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.adapter.synchronization.provider.JedisPoolProvider;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.service.SynchronizationService;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@SuppressWarnings("resource")
public class RedisSynchronizationService implements SynchronizationService {
	private static final String KV_PREFIX = "identica";

	private final JedisPoolProvider poolProvider;
	private final Provider<Replication> replicationProvider;

	@Override
	public boolean isAvailable() {
		return replicationProvider.get().isEnabled();
	}

	@Override
	public @NotNull CompletableFuture<Optional<byte[]>> get(@NotNull String namespace, @NotNull String key) {
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
	public @NotNull CompletableFuture<Void> put(@NotNull String namespace, @NotNull String key, byte[] value, long ttlMs) {
		if (!isAvailable()) return CompletableFuture.completedFuture(null);

		return CompletableFuture.runAsync(() -> {
			JedisPool pool = poolProvider.getOptional().orElse(null);
			if (pool == null) {
				return;
			}
			byte[] rawKey = buildKey(namespace, key);
			try (Jedis jedis = pool.getResource()) {
				if (ttlMs > 0) {
					jedis.psetex(rawKey, ttlMs, value);
					long expiresAt = System.currentTimeMillis() + ttlMs;
					jedis.zadd(buildIndexKey(namespace), expiresAt, key);
				} else {
					jedis.set(rawKey, value);
				}
			} catch (Exception ignored) {
				// ignore failures for cache
			}
		});
	}

	@Override
	public @NotNull CompletableFuture<Void> invalidate(@NotNull String namespace, @NotNull String key) {
		if (!isAvailable()) return CompletableFuture.completedFuture(null);

		return CompletableFuture.runAsync(() -> {
			JedisPool pool = poolProvider.getOptional().orElse(null);
			if (pool == null) {
				return;
			}
			byte[] rawKey = buildKey(namespace, key);
			try (Jedis jedis = pool.getResource()) {
				jedis.del(rawKey);
				jedis.zrem(buildIndexKey(namespace), key);
			} catch (Exception ignored) {
				// ignore failures for sync
			}
		});
	}

	@Override
	public @NotNull CompletableFuture<Cache.Page> listKeys(
			@NotNull String namespace,
			int page,
			int pageSize
	) {
		int safePage = Math.max(1, page);
		int safeSize = Math.max(1, pageSize);

		if (!isAvailable()) {
			return CompletableFuture.completedFuture(new Cache.Page(List.of(), safePage, safeSize, 0));
		}

		return CompletableFuture.supplyAsync(() -> {
			JedisPool pool = poolProvider.getOptional().orElse(null);
			if (pool == null) {
				return new Cache.Page(List.of(), safePage, safeSize, 0);
			}
			long now = System.currentTimeMillis();
			String indexKey = buildIndexKey(namespace);

			try (Jedis jedis = pool.getResource()) {
				jedis.zremrangeByScore(indexKey, 0, now);
				long total = jedis.zcard(indexKey);

				int fromIndex = Math.min((safePage - 1) * safeSize, (int) total);
				int toIndex = Math.min(fromIndex + safeSize - 1, (int) total - 1);

				if (total == 0 || fromIndex > toIndex) {
					return new Cache.Page(List.of(), safePage, safeSize, (int) total);
				}

				List<String> members = jedis.zrange(indexKey, fromIndex, toIndex);
				return new Cache.Page(members, safePage, safeSize, (int) total);
			} catch (Exception ignored) {
				return new Cache.Page(List.of(), safePage, safeSize, 0);
			}
		});
	}

	@Override
	public @NotNull CompletableFuture<Void> publish(@NotNull String channel, byte[] payload) {
		if (!isAvailable() || channel.isBlank() || payload == null) return CompletableFuture.completedFuture(null);
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
	public void subscribe(@NotNull String channel, java.util.function.@NotNull Consumer<byte[]> handler) {
		if (!isAvailable() || channel.isBlank()) return;

		JedisPool pool = poolProvider.getOptional().orElse(null);
		if (pool == null) return;

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

	private String buildIndexKey(String namespace) {
		StringBuilder builder = new StringBuilder(KV_PREFIX).append(":index");
		if (namespace != null && !namespace.isBlank()) {
			builder.append(':').append(namespace);
		}
		return builder.toString();
	}
}
