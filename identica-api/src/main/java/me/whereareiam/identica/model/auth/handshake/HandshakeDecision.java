package me.whereareiam.identica.model.auth.handshake;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class HandshakeDecision {
	private final Status status;
	private final String message;
	private final String reason;

	public static HandshakeDecision allow() {
		return new HandshakeDecision(Status.ALLOW, null, null);
	}

	public static HandshakeDecision deny(String message) {
		return new HandshakeDecision(Status.DENY, message, null);
	}

	public static HandshakeDecision forceOnline() {
		return new HandshakeDecision(Status.FORCE_ONLINE, null, null);
	}

	public static HandshakeDecision forceOffline() {
		return new HandshakeDecision(Status.FORCE_OFFLINE, null, null);
	}

	public enum Status {
		ALLOW,
		DENY,
		FORCE_ONLINE,
		FORCE_OFFLINE
	}
}
