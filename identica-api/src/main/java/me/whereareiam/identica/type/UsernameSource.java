package me.whereareiam.identica.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Source of the currently active username.
 */
@RequiredArgsConstructor
public enum UsernameSource {
	PROVIDER("provider"),
	MANUAL("manual"),
	SYSTEM("system");

	@Getter
	private final @NotNull String id;

	/**
	 * Resolve a source from its identifier.
	 *
	 * @param id source identifier
	 * @return resolved source or {@link #PROVIDER} as default
	 */
	public static @NotNull UsernameSource fromId(@Nullable String id) {
		if (id == null || id.isBlank()) return PROVIDER;

		String normalized = id.trim().toLowerCase();
		for (UsernameSource source : values())
			if (source.id.equals(normalized))
				return source;

		return PROVIDER;
	}
}
