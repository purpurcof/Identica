package me.whereareiam.identica.service;

import me.whereareiam.identica.model.delivery.DeliveryRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for queued delivery requests and their lookup indexes.
 */
public interface DeliveryStore {
	/**
	 * Stores a delivery request and updates any secondary indexes required to
	 * resolve it later by connection or account.
	 *
	 * @param request delivery request to persist
	 */
	void put(@NotNull DeliveryRequest request);

	/**
	 * Resolves all stored requests targeting the given connection.
	 *
	 * @param connectionUniqueId connection identifier to query
	 * @return pending requests addressed to the connection
	 */
	@NotNull List<DeliveryRequest> findByConnectionUniqueId(@NotNull UUID connectionUniqueId);

	/**
	 * Resolves all stored requests targeting the given account.
	 *
	 * @param accountUniqueId account identifier to query
	 * @return pending requests addressed to the account
	 */
	@NotNull List<DeliveryRequest> findByAccountUniqueId(@NotNull UUID accountUniqueId);

	/**
	 * Looks up a stored request by its delivery identifier.
	 *
	 * @param deliveryUniqueId delivery identifier to query
	 * @return matching request when it exists in storage
	 */
	@NotNull Optional<DeliveryRequest> findById(@NotNull UUID deliveryUniqueId);

	/**
	 * Removes a stored request and any related index entries.
	 *
	 * @param deliveryUniqueId delivery identifier to remove
	 * @return {@code true} when a request was removed
	 */
	boolean remove(@NotNull UUID deliveryUniqueId);
}
