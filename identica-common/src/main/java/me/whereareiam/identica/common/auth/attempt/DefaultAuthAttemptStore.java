package me.whereareiam.identica.common.auth.attempt;

import com.google.inject.Singleton;
import me.whereareiam.identica.auth.attempt.AuthAttempt;
import me.whereareiam.identica.auth.attempt.AuthAttemptStore;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultAuthAttemptStore implements AuthAttemptStore {
	private final Map<UUID, Entry> attempts = new ConcurrentHashMap<>();

	@Override
	public void store(@NotNull AuthAttempt attempt, long ttlMs) {
		long expiresAt = ttlMs > 0 ? System.currentTimeMillis() + ttlMs : 0;
		attempts.put(attempt.getAttemptId(), new Entry(attempt, expiresAt));
	}

	@Override
	public @NotNull Optional<AuthAttempt> find(@NotNull UUID attemptId) {
		cleanupExpired();
		Entry entry = attempts.get(attemptId);
		if (entry == null) return Optional.empty();
		if (isExpired(entry)) {
			attempts.remove(attemptId, entry);
			return Optional.empty();
		}
		return Optional.of(entry.attempt);
	}

	@Override
	public @NotNull Optional<AuthAttempt> consume(@NotNull ResumeRequest request) {
		cleanupExpired();
		AuthAttempt attempt = findMatching(request, true);
		return Optional.ofNullable(attempt);
	}

	@Override
	public boolean hasPending(@NotNull ResumeRequest request) {
		cleanupExpired();
		return findMatching(request, false) != null;
	}

	@Override
	public void clear(@NotNull UUID attemptId) {
		attempts.remove(attemptId);
	}

	private @Nullable AuthAttempt findMatching(@NotNull ResumeRequest request, boolean consume) {
		UUID connectionId = request.getConnectionUniqueId();
		if (connectionId != null) {
			AuthAttempt byConnection = findByConnection(connectionId, consume);
			if (byConnection != null) return byConnection;
		}

		String username = request.getUsername();
		if (username == null || username.isBlank()) return null;
		return findByUsername(username, consume);
	}

	private @Nullable AuthAttempt findByConnection(@NotNull UUID connectionId, boolean consume) {
		for (Map.Entry<UUID, Entry> entry : attempts.entrySet()) {
			AuthAttempt attempt = entry.getValue().attempt;
			UUID candidate = attempt.getContext().getConnectionUniqueId();
			if (candidate == null || !candidate.equals(connectionId)) continue;
			if (consume) attempts.remove(entry.getKey(), entry.getValue());
			return attempt;
		}
		return null;
	}

	private @Nullable AuthAttempt findByUsername(@NotNull String username, boolean consume) {
		String target = username.trim();
		for (Map.Entry<UUID, Entry> entry : attempts.entrySet()) {
			AuthAttempt attempt = entry.getValue().attempt;
			String candidate = attempt.getContext().getUsername();
			if (candidate == null || !candidate.equalsIgnoreCase(target)) continue;
			if (consume) attempts.remove(entry.getKey(), entry.getValue());
			return attempt;
		}
		return null;
	}

	private void cleanupExpired() {
		long now = System.currentTimeMillis();
		attempts.entrySet().removeIf(entry -> entry.getValue().expiresAt > 0 && entry.getValue().expiresAt <= now);
	}

	private boolean isExpired(@NotNull Entry entry) {
		return entry.expiresAt > 0 && entry.expiresAt <= System.currentTimeMillis();
	}

	private record Entry(AuthAttempt attempt, long expiresAt) {
	}
}
