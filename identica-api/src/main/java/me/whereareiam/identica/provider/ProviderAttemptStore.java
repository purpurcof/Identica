package me.whereareiam.identica.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks provider-specific attempt markers to avoid repeated reconnect loops.
 */
public interface ProviderAttemptStore {
	boolean hasAttempt(
			@NotNull String providerId,
			@NotNull String scope,
			@Nullable String username,
			@Nullable String ip
	);

	void markAttempt(
			@NotNull String providerId,
			@NotNull String scope,
			@Nullable String username,
			@Nullable String ip
	);

	void clearAttempt(
			@NotNull String providerId,
			@NotNull String scope,
			@Nullable String username,
			@Nullable String ip
	);
}
