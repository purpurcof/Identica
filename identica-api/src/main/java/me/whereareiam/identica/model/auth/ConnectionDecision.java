package me.whereareiam.identica.model.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class ConnectionDecision {
	private final Status status;
	private final String message;
	private final String reason;

	public static ConnectionDecision allow() {
		return new ConnectionDecision(Status.ALLOW, null, null);
	}

	public static ConnectionDecision waiting(String message) {
		return new ConnectionDecision(Status.WAIT, message, null);
	}

	public static ConnectionDecision deny(String message) {
		return new ConnectionDecision(Status.DENY, message, null);
	}

	public static ConnectionDecision requireReconnect(String message) {
		return new ConnectionDecision(Status.REQUIRE_RECONNECT, message, null);
	}

	public static ConnectionDecision noPending() {
		return new ConnectionDecision(Status.NO_PENDING, null, null);
	}

	public enum Status {
		ALLOW,
		WAIT,
		DENY,
		REQUIRE_RECONNECT,
		NO_PENDING
	}
}
