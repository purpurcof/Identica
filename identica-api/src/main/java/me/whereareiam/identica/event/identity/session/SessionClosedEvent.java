package me.whereareiam.identica.event.identity.session;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.ReplicatedEvent;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.SessionCloseRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired after a session is closed.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class SessionClosedEvent implements ReplicatedEvent, SynchronousEvent {
	private @NotNull UUID uniqueId;
	private @Nullable Session session;
	private @NotNull SessionCloseRequest request;
	private @Nullable UUID replicationEventId;
	private @Nullable String replicationOriginServerId;

	/**
	 * Creates a session close event.
	 *
	 * @param uniqueId identity id whose session was closed
	 * @param session closed session, when it was known
	 */
	public SessionClosedEvent(@NotNull UUID uniqueId, @Nullable Session session) {
		this(uniqueId, session, SessionCloseRequest.builder()
				.uniqueId(uniqueId)
				.build());
	}

	/**
	 * Creates a session close event with the originating request.
	 *
	 * @param uniqueId identity id whose session was closed
	 * @param session closed session, when it was known
	 * @param request close request
	 */
	public SessionClosedEvent(
			@NotNull UUID uniqueId,
			@Nullable Session session,
			@NotNull SessionCloseRequest request
	) {
		this.uniqueId = uniqueId;
		this.session = session;
		this.request = request;
		this.replicationEventId = request.getRequestId();
		this.replicationOriginServerId = request.getOriginServerId();
	}
}
