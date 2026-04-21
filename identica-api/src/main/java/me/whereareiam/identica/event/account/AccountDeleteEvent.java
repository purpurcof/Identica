package me.whereareiam.identica.event.account;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when an account delete is requested.
 */
public class AccountDeleteEvent extends AccountLifecycleEvent {
	/**
	 * Creates a non-synchronized delete event.
	 *
	 * @param identity target identity
	 */
	public AccountDeleteEvent(@NotNull ConnectionIdentity identity) {
		super(identity);
	}

	/**
	 * Creates a delete event.
	 *
	 * @param identity target identity
	 * @param synchronizedEvent whether this event came from synchronization
	 */
	public AccountDeleteEvent(@NotNull ConnectionIdentity identity, boolean synchronizedEvent) {
		super(identity, synchronizedEvent);
	}

	/**
	 * Creates a delete event with replication metadata.
	 *
	 * @param identity target identity
	 * @param synchronizedEvent whether this event came from synchronization
	 * @param replicationEventId replication event id
	 * @param replicationOriginServerId origin server id
	 */
	public AccountDeleteEvent(
			@NotNull ConnectionIdentity identity,
			boolean synchronizedEvent,
			@Nullable UUID replicationEventId,
			@Nullable String replicationOriginServerId
	) {
		super(identity, synchronizedEvent, replicationEventId, replicationOriginServerId);
	}
}
