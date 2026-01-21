package me.whereareiam.identica.common.auth.handshake;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.auth.HandshakeDirective;
import me.whereareiam.identica.type.HandshakeMode;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class HandshakeDirectiveStore {
	private static final long DEFAULT_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

	private final ConcurrentHashMap<String, HandshakeDirective> pending = new ConcurrentHashMap<>();

	public long getDefaultTtlMillis() {
		return DEFAULT_TTL_MILLIS;
	}

	public void put(HandshakeDirective directive) {
		if (directive == null || directive.getUsername() == null || directive.getMode() == null)
			return;

		String key = normalize(directive.getUsername());
		if (key == null) return;

		pending.put(key, directive);
	}

	public void request(String username, HandshakeMode mode) {
		if (username == null || mode == null) return;
		put(HandshakeDirective.create(username, mode, DEFAULT_TTL_MILLIS));
	}

	public Optional<HandshakeDirective> peek(String username) {
		return read(username, false);
	}

	public Optional<HandshakeDirective> consume(String username) {
		return read(username, true);
	}

	private Optional<HandshakeDirective> read(String username, boolean consume) {
		String key = normalize(username);
		if (key == null) return Optional.empty();

		HandshakeDirective directive = pending.get(key);
		if (directive == null) return Optional.empty();

		if (directive.isExpired(System.currentTimeMillis())) {
			pending.remove(key);
			return Optional.empty();
		}

		if (consume) pending.remove(key);
		return Optional.of(directive);
	}

	private String normalize(String username) {
		if (username == null || username.isBlank()) return null;
		return username.trim().toLowerCase();
	}
}
