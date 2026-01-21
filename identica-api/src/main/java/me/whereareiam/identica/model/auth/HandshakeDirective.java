package me.whereareiam.identica.model.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.type.HandshakeMode;

@Getter
@ToString
@RequiredArgsConstructor
public class HandshakeDirective {
	private final String username;
	private final HandshakeMode mode;
	private final long expiresAt;

	public static HandshakeDirective create(String username, HandshakeMode mode, long ttlMillis) {
		long expiresAt = System.currentTimeMillis() + ttlMillis;
		return new HandshakeDirective(username, mode, expiresAt);
	}

	public boolean isExpired(long now) {
		return expiresAt <= now;
	}
}
