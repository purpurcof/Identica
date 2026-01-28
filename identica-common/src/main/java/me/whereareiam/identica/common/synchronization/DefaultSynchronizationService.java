package me.whereareiam.identica.common.synchronization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.service.SynchronizationService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Singleton
public class DefaultSynchronizationService implements SynchronizationService {
	private final @NotNull SynchronizationService provider;

	@Inject
	public DefaultSynchronizationService(
			@Named("synchronizationProvider") @NotNull SynchronizationService provider
	) {
		this.provider = provider;
	}

	@Override
	public boolean isAvailable() {
		return provider.isAvailable();
	}

	@Override
	public @NotNull CompletableFuture<Optional<byte[]>> get(@NotNull String namespace, @NotNull String key) {
		if (!provider.isAvailable()) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		return provider.get(namespace, key);
	}

	@Override
	public @NotNull CompletableFuture<Void> put(
			@NotNull String namespace,
			@NotNull String key,
			byte[] value,
			long ttlMs
	) {
		if (!provider.isAvailable()) {
			return CompletableFuture.completedFuture(null);
		}

		return provider.put(namespace, key, value, ttlMs);
	}

	@Override
	public @NotNull CompletableFuture<Void> invalidate(@NotNull String namespace, @NotNull String key) {
		if (!provider.isAvailable()) {
			return CompletableFuture.completedFuture(null);
		}

		return provider.invalidate(namespace, key);
	}

	@Override
	public @NotNull CompletableFuture<Cache.Page> listKeys(
			@NotNull String namespace,
			int page,
			int pageSize
	) {
		if (!provider.isAvailable()) {
			int safePage = Math.max(1, page);
			int safeSize = Math.max(1, pageSize);
			return CompletableFuture.completedFuture(new Cache.Page(List.of(), safePage, safeSize, 0));
		}

		return provider.listKeys(namespace, page, pageSize);
	}

	@Override
	public @NotNull CompletableFuture<Void> publish(@NotNull String channel, byte @NotNull [] payload) {
		if (!provider.isAvailable()) {
			return CompletableFuture.completedFuture(null);
		}

		return provider.publish(channel, payload);
	}

	@Override
	public void subscribe(@NotNull String channel, @NotNull Consumer<byte[]> handler) {
		if (!provider.isAvailable()) return;
		provider.subscribe(channel, handler);
	}
}
