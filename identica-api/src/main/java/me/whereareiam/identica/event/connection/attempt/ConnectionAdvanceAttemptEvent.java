package me.whereareiam.identica.event.connection.attempt;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ConnectionAdvanceAttemptEvent extends ConnectionAttemptEvent {
	public ConnectionAdvanceAttemptEvent(
			@Nullable UUID connectionUniqueId,
			@Nullable UUID accountUniqueId,
			@Nullable String username,
			@Nullable String ip
	) {
		super(connectionUniqueId, accountUniqueId, username, ip, null);
	}
}
