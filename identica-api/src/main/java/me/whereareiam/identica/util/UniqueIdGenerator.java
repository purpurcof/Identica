package me.whereareiam.identica.util;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.identity.UniqueIdMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Utility for generating Identica and offline-mode UUIDs.
 */
@Singleton
public class UniqueIdGenerator {
	private static final SplittableRandom RNG = new SplittableRandom();

	private final Provider<Settings> settingsProvider;

	/**
	 * Creates a UUID generator backed by the live settings provider.
	 *
	 * @param settingsProvider settings provider used to resolve the configured UUID mode
	 */
	@Inject
	public UniqueIdGenerator(@NotNull Provider<Settings> settingsProvider) {
		this.settingsProvider = settingsProvider;
	}

	/**
	 * Resolves a new account UUID using the configured assignment mode.
	 *
	 * @param username player username
	 * @param providerSubject provider subject, usually the premium UUID for premium accounts
	 * @param observedUniqueId UUID observed from the platform profile request
	 * @return resolved UUID or {@code null} when the configured mode cannot resolve one
	 */
	public @Nullable UUID resolveConfiguredUniqueId(
			@NotNull String username,
			@Nullable String providerSubject,
			@Nullable UUID observedUniqueId
	) {
		return resolveUniqueId(uniqueIdMode(), username, providerSubject, observedUniqueId);
	}

	/**
	 * Returns whether account creation must ignore platform fallback UUIDs and use the configured resolver.
	 *
	 * @return {@code true} when the configured mode is strict
	 */
	public boolean requiresConfiguredUniqueId() {
		return uniqueIdMode() == UniqueIdMode.PREMIUM;
	}

	/**
	 * Generates a new Identica unique id.
	 *
	 * @return generated unique id
	 */
	public static UUID newIdenticaUniqueId() {
		long msb = RNG.nextLong();
		long lsb = RNG.nextLong();

		lsb = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L;
		msb = (msb & 0xffffffffffff0fffL) | 0x0000000000008000L;

		return new UUID(msb, lsb);
	}

	/**
	 * Resolves a new account UUID using the configured assignment mode.
	 *
	 * @param mode configured UUID assignment mode
	 * @param username player username
	 * @param providerSubject provider subject, usually the premium UUID for premium accounts
	 * @param observedUniqueId UUID observed from the platform profile request
	 * @return resolved UUID or {@code null} when the configured mode cannot resolve one
	 */
	public static @Nullable UUID resolveUniqueId(
			@Nullable UniqueIdMode mode,
			@NotNull String username,
			@Nullable String providerSubject,
			@Nullable UUID observedUniqueId
	) {
		UniqueIdMode resolvedMode = mode != null ? mode : UniqueIdMode.RANDOM;
		return switch (resolvedMode) {
			case RANDOM -> newIdenticaUniqueId();
			case OFFLINE -> offlinePlayerUniqueId(username);
			case PREMIUM -> premiumUniqueId(username, providerSubject, observedUniqueId);
		};
	}

	/**
	 * Generates an offline-mode UUID using the standard OfflinePlayer namespace.
	 *
	 * @param username player username
	 * @return derived UUID or {@code null} when the username is blank
	 */
	public static @Nullable UUID offlinePlayerUniqueId(@NotNull String username) {
		if (username.isBlank()) return null;
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
	}

	private static @Nullable UUID premiumUniqueId(
			@NotNull String username,
			@Nullable String providerSubject,
			@Nullable UUID observedUniqueId
	) {
		UUID parsed = UniqueIdUtil.parseUniqueId(providerSubject);
		if (parsed != null) return parsed;

		UUID offlineUniqueId = offlinePlayerUniqueId(username);
		if (observedUniqueId == null || observedUniqueId.equals(offlineUniqueId)) return null;

		return observedUniqueId;
	}

	private @NotNull UniqueIdMode uniqueIdMode() {
		Settings settings = settingsProvider.get();
		if (settings == null) return UniqueIdMode.RANDOM;

		return settings.getIdentity().getUniqueIdMode();
	}
}
