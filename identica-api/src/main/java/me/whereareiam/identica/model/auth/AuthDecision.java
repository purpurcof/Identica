package me.whereareiam.identica.model.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.type.HandshakeMode;

@Getter
@ToString
@RequiredArgsConstructor
public class AuthDecision {
	private final Status status;
	private final String message;
	private final String reason;
	private final HandshakeMode handshakeMode;

	public static AuthDecision allow() {
		return new AuthDecision(Status.ALLOW, null, null, null);
	}

	public static AuthDecision waiting(String message) {
		return new AuthDecision(Status.WAIT, message, null, null);
	}

	public static AuthDecision deny(String message) {
		return new AuthDecision(Status.DENY, message, null, null);
	}

	public static AuthDecision requireReconnect(HandshakeMode mode, String message) {
		return new AuthDecision(Status.REQUIRE_RECONNECT, message, null, mode);
	}

	public static AuthDecision noPending() {
		return new AuthDecision(Status.NO_PENDING, null, null, null);
	}

	public enum Status {
		ALLOW,
		WAIT,
		DENY,
		REQUIRE_RECONNECT,
		NO_PENDING
	}
}
