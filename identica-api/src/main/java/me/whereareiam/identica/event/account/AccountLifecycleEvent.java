package me.whereareiam.identica.event.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.ReplicatedEvent;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when an account lifecycle operation is requested.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AccountLifecycleEvent implements ReplicatedEvent, SynchronousEvent {
	private @NotNull ConnectionIdentity identity;
	private boolean synchronizedEvent;
	private @Nullable UUID replicationEventId;
	private @Nullable String replicationOriginServerId;

	/**
	 * Creates a non-synchronized lifecycle event.
	 *
	 * @param identity target identity
	 */
	public AccountLifecycleEvent(@NotNull ConnectionIdentity identity) {
		this(identity, false, null, null);
	}

	/**
	 * Creates a lifecycle event.
	 *
	 * @param identity target identity
	 * @param synchronizedEvent whether this event came from synchronization
	 */
	public AccountLifecycleEvent(@NotNull ConnectionIdentity identity, boolean synchronizedEvent) {
		this(identity, synchronizedEvent, null, null);
	}

	/**
	 * Creates a lifecycle event with replication metadata.
	 *
	 * @param identity target identity
	 * @param synchronizedEvent whether this event came from synchronization
	 * @param replicationEventId replication event id
	 * @param replicationOriginServerId origin server id
	 */
	public AccountLifecycleEvent(
			@NotNull ConnectionIdentity identity,
			boolean synchronizedEvent,
			@Nullable UUID replicationEventId,
			@Nullable String replicationOriginServerId
	) {
		this.identity = identity;
		this.synchronizedEvent = synchronizedEvent;
		this.replicationEventId = replicationEventId;
		this.replicationOriginServerId = replicationOriginServerId;
	}
}
