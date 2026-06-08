package me.whereareiam.identica.type.provider.capability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Normalized provider capability identifier.
 *
 * <p>Capabilities are resolved from provider-supplied bootstraps after the
 * provider has declared and loaded its required runtime libraries.</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ProviderCapability {
	private final @NotNull String id;

	private ProviderCapability(@NotNull String id) {
		this.id = normalize(id);
	}

	/**
	 * Creates a capability identifier from a raw id.
	 *
	 * @param id raw capability id
	 * @return normalized capability id
	 */
	public static @NotNull ProviderCapability of(@NotNull String id) {
		return new ProviderCapability(id);
	}

	/**
	 * Checks whether a raw id matches this capability.
	 *
	 * @param candidate raw capability id
	 * @return {@code true} when the id matches
	 */
	public boolean matches(@Nullable String candidate) {
		if (candidate == null || candidate.isBlank()) return false;
		return id.equals(normalize(candidate));
	}

	private static @NotNull String normalize(@NotNull String value) {
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) throw new IllegalArgumentException("Provider capability id cannot be blank");

		return normalized;
	}
}
