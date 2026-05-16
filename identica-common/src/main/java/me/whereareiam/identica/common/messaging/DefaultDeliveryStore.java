package me.whereareiam.identica.common.messaging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import me.whereareiam.identica.service.DeliveryStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class DefaultDeliveryStore implements DeliveryStore {
	private final ReplicatedCache<DeliveryRequest> requests;
	private final ReplicatedCache<DeliveryRequestKeyIndex> connectionIndex;
	private final ReplicatedCache<DeliveryRequestKeyIndex> accountIndex;
	private final Provider<Settings> settingsProvider;

	@Inject
	public DefaultDeliveryStore(
			@NotNull ReplicationSystem replicationSystem,
			@NotNull Provider<Replication> replicationProvider,
			@NotNull Provider<Settings> settingsProvider
	) {
		Replication.Delivery delivery = replicationProvider.get().getCache().getDelivery();
		var indexType = ReplicationType.identity(DeliveryRequestKeyIndex.class);

		this.requests = replicationSystem.cache(delivery.getRequests()).replicated(ReplicationType.identity(DeliveryRequest.class));
		this.connectionIndex = replicationSystem.cache(delivery.getConnectionIndex()).replicated(indexType);
		this.accountIndex = replicationSystem.cache(delivery.getAccountIndex()).replicated(indexType);
		this.settingsProvider = settingsProvider;
	}

	@Override
	public void put(@NotNull DeliveryRequest request) {
		String requestKey = key(request.getId());
		requests.put(requestKey, request, ttlMs()).join();

		UUID connectionUniqueId = request.getTarget().getConnectionUniqueId();
		if (connectionUniqueId != null)
			putIndex(connectionIndex, key(connectionUniqueId), requestKey);

		UUID accountUniqueId = request.getTarget().getAccountUniqueId();
		if (accountUniqueId != null)
			putIndex(accountIndex, key(accountUniqueId), requestKey);
	}

	@Override
	public @NotNull List<DeliveryRequest> findByConnectionUniqueId(@NotNull UUID connectionUniqueId) {
		return resolve(connectionIndex, key(connectionUniqueId));
	}

	@Override
	public @NotNull List<DeliveryRequest> findByAccountUniqueId(@NotNull UUID accountUniqueId) {
		return resolve(accountIndex, key(accountUniqueId));
	}

	@Override
	public @NotNull Optional<DeliveryRequest> findById(@NotNull UUID deliveryUniqueId) {
		return requests.get(key(deliveryUniqueId)).join();
	}

	@Override
	public boolean remove(@NotNull UUID deliveryUniqueId) {
		DeliveryRequest request = requests.consume(key(deliveryUniqueId)).join().orElse(null);
		if (request == null) return false;

		UUID connectionUniqueId = request.getTarget().getConnectionUniqueId();
		if (connectionUniqueId != null) removeIndex(connectionIndex, key(connectionUniqueId), key(deliveryUniqueId));

		UUID accountUniqueId = request.getTarget().getAccountUniqueId();
		if (accountUniqueId != null) removeIndex(accountIndex, key(accountUniqueId), key(deliveryUniqueId));

		return true;
	}

	private @NotNull List<DeliveryRequest> resolve(
			@NotNull ReplicatedCache<DeliveryRequestKeyIndex> index,
			@NotNull String indexKey
	) {
		List<String> requestKeys = index.get(indexKey).join()
				.map(DeliveryRequestKeyIndex::getValues)
				.orElse(List.of());
		if (requestKeys.isEmpty()) return List.of();

		List<DeliveryRequest> resolved = new ArrayList<>();
		List<String> retained = new ArrayList<>();
		for (String requestKey : requestKeys) {
			DeliveryRequest request = requests.get(requestKey).join().orElse(null);
			if (request == null) continue;

			resolved.add(request);
			retained.add(requestKey);
		}

		if (retained.size() != requestKeys.size())
			index.put(indexKey, DeliveryRequestKeyIndex.builder().values(retained).build(), ttlMs()).join();

		return List.copyOf(resolved);
	}

	private void putIndex(
			@NotNull ReplicatedCache<DeliveryRequestKeyIndex> index,
			@NotNull String indexKey,
			@NotNull String requestKey
	) {
		List<String> values = new ArrayList<>(index.get(indexKey).join()
				.map(DeliveryRequestKeyIndex::getValues)
				.orElse(List.of()));
		if (!values.contains(requestKey)) values.add(requestKey);

		index.put(indexKey, DeliveryRequestKeyIndex.builder().values(values).build(), ttlMs()).join();
	}

	private void removeIndex(
			@NotNull ReplicatedCache<DeliveryRequestKeyIndex> index,
			@NotNull String indexKey,
			@NotNull String requestKey
	) {
		List<String> values = new ArrayList<>(index.get(indexKey).join()
				.map(DeliveryRequestKeyIndex::getValues)
				.orElse(List.of()));

		values.removeIf(requestKey::equals);
		if (values.isEmpty()) {
			index.invalidate(indexKey).join();
			return;
		}

		index.put(indexKey, DeliveryRequestKeyIndex.builder().values(values).build(), ttlMs()).join();
	}

	private @NotNull String key(@NotNull UUID uniqueId) {
		return uniqueId.toString();
	}

	private long ttlMs() {
		return settingsProvider.get().getConnection().prepareStateTtlMillis();
	}
}
