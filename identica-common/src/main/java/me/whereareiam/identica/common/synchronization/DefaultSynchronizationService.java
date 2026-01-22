package me.whereareiam.identica.common.synchronization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.synchronization.SynchronizationService;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Singleton
public class DefaultSynchronizationService implements SynchronizationService {
	private final Set<SynchronizationService> providers;

	@Inject
	public DefaultSynchronizationService(
			@Named("synchronizationProviders") Set<SynchronizationService> providers
	) {
		this.providers = providers;
	}

	@Override
	public boolean isAvailable() {
		return selectProvider() != null;
	}

	@Override
	public CompletableFuture<Optional<byte[]>> get(String namespace, String key) {
		SynchronizationService provider = selectProvider();
		if (provider == null) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		return provider.get(namespace, key);
	}

	@Override
	public CompletableFuture<Void> put(String namespace, String key, byte[] value, long ttlMs) {
		SynchronizationService provider = selectProvider();
		if (provider == null) {
			return CompletableFuture.completedFuture(null);
		}

		return provider.put(namespace, key, value, ttlMs);
	}

	@Override
	public CompletableFuture<Void> invalidate(String namespace, String key) {
		SynchronizationService provider = selectProvider();
		if (provider == null) {
			return CompletableFuture.completedFuture(null);
		}

		return provider.invalidate(namespace, key);
	}

	@Override
	public CompletableFuture<Void> publish(String channel, byte[] payload) {
		SynchronizationService provider = selectProvider();
		if (provider == null) {
			return CompletableFuture.completedFuture(null);
		}

		return provider.publish(channel, payload);
	}

	@Override
	public void subscribe(String channel, Consumer<byte[]> handler) {
		SynchronizationService provider = selectProvider();
		if (provider == null) return;

		provider.subscribe(channel, handler);
	}

	private SynchronizationService selectProvider() {
		for (SynchronizationService provider : providers)
			if (provider != null && provider.isAvailable())
				return provider;

		return null;
	}
}
