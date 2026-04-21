package me.whereareiam.identica.event.base;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event marker for facts that should be replayed on replicated instances.
 *
 * <p>Only events that are safe for every instance to handle locally should
 * implement this interface. Global one-time operations should remain commands
 * or services instead of replicated events.</p>
 */
public interface ReplicatedEvent extends Event {
	/**
	 * Returns the unique replication event id used for deduplication.
	 *
	 * @return replication event id
	 */
	@Nullable UUID getReplicationEventId();

	/**
	 * Sets the unique replication event id used for deduplication.
	 *
	 * @param replicationEventId replication event id
	 */
	void setReplicationEventId(@Nullable UUID replicationEventId);

	/**
	 * Returns the server id that originally published this event.
	 *
	 * @return origin server id
	 */
	@Nullable String getReplicationOriginServerId();

	/**
	 * Sets the server id that originally published this event.
	 *
	 * @param replicationOriginServerId origin server id
	 */
	void setReplicationOriginServerId(@Nullable String replicationOriginServerId);
}
