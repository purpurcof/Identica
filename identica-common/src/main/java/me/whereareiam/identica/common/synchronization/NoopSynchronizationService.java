package me.whereareiam.identica.common.synchronization;

import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.service.SynchronizationService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * No-op synchronization provider used when no backend is configured.
 */
public class NoopSynchronizationService implements SynchronizationService {
	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public @NotNull CompletableFuture<Optional<byte[]>> get(@NotNull String namespace, @NotNull String key) {
		return CompletableFuture.completedFuture(Optional.empty());
	}

	@Override
	public @NotNull CompletableFuture<Optional<byte[]>> consume(@NotNull String namespace, @NotNull String key) {
		return CompletableFuture.completedFuture(Optional.empty());
	}

	@Override
	public @NotNull CompletableFuture<Void> put(@NotNull String namespace, @NotNull String key, byte[] value, long ttlMs) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public @NotNull CompletableFuture<Void> invalidate(@NotNull String namespace, @NotNull String key) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public @NotNull CompletableFuture<Cache.Page> listKeys(
			@NotNull String namespace,
			int page,
			int pageSize
	) {
		int safePage = Math.max(1, page);
		int safeSize = Math.max(1, pageSize);
		return CompletableFuture.completedFuture(new Cache.Page(List.of(), safePage, safeSize, 0));
	}

	@Override
	public @NotNull CompletableFuture<Void> publish(@NotNull String channel, byte[] payload) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void subscribe(@NotNull String channel, @NotNull Consumer<byte[]> handler) {
	}
}
