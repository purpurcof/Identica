package me.whereareiam.identica.service;

import me.whereareiam.identica.model.delivery.DeliveryDispatchContext;
import me.whereareiam.identica.model.delivery.DeliveryRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates queued delivery work and dispatches requests when their
 * checkpoint and routing requirements have been satisfied.
 */
public interface DeliveryService {
	/**
	 * Queues a delivery request for later dispatch.
	 *
	 * @param request delivery request to store
	 */
	void queue(@NotNull DeliveryRequest request);

	/**
	 * Returns all pending delivery requests that target the given connection.
	 *
	 * @param connectionUniqueId connection identifier to query
	 * @return immutable view of pending requests for the connection
	 */
	@NotNull List<DeliveryRequest> pendingForConnection(@NotNull UUID connectionUniqueId);

	/**
	 * Returns all pending delivery requests that target the given account.
	 *
	 * @param accountUniqueId account identifier to query
	 * @return immutable view of pending requests for the account
	 */
	@NotNull List<DeliveryRequest> pendingForAccount(@NotNull UUID accountUniqueId);

	/**
	 * Dispatches all pending requests that match the supplied context.
	 *
	 * @param context dispatch checkpoint, identity, and server context
	 * @return requests that were dispatched during this pass
	 */
	@NotNull List<DeliveryRequest> dispatch(@NotNull DeliveryDispatchContext context);

	/**
	 * Acknowledges a delivery request and removes it from the queue.
	 *
	 * @param deliveryUniqueId queued delivery identifier
	 * @param reason diagnostic reason for the acknowledgement
	 */
	void acknowledge(@NotNull UUID deliveryUniqueId, @NotNull String reason);

	/**
	 * Removes all pending requests scoped to a connection.
	 *
	 * @param connectionUniqueId connection identifier whose requests should be removed
	 * @param reason diagnostic reason for the invalidation
	 */
	void invalidateByConnection(@NotNull UUID connectionUniqueId, @NotNull String reason);

	/**
	 * Removes all pending requests scoped to an account.
	 *
	 * @param accountUniqueId account identifier whose requests should be removed
	 * @param reason diagnostic reason for the invalidation
	 */
	void invalidateByAccount(@NotNull UUID accountUniqueId, @NotNull String reason);

	/**
	 * Looks up a queued request by its delivery identifier.
	 *
	 * @param deliveryUniqueId queued delivery identifier
	 * @return matching request when it is still pending
	 */
	@NotNull Optional<DeliveryRequest> find(@NotNull UUID deliveryUniqueId);
}
