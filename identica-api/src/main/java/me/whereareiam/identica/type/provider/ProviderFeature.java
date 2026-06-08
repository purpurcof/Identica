package me.whereareiam.identica.type.provider;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Stable identifier for a built-in provider feature.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ProviderFeature {
	private final @NotNull String id;

	private ProviderFeature(@NotNull String id) {
		String normalized = id.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank())
			throw new IllegalArgumentException("feature id cannot be blank");

		this.id = normalized;
	}

	/**
	 * Creates a normalized feature identifier.
	 *
	 * @param id raw feature id
	 * @return normalized feature id
	 */
	public static @NotNull ProviderFeature of(@NotNull String id) {
		return new ProviderFeature(id);
	}

	/**
	 * Checks whether the supplied id matches this feature.
	 *
	 * @param candidate raw candidate id
	 * @return {@code true} when the ids match
	 */
	public boolean matches(@Nullable String candidate) {
		if (candidate == null || candidate.isBlank()) return false;

		return id.equals(candidate.trim().toLowerCase(Locale.ROOT));
	}
}
