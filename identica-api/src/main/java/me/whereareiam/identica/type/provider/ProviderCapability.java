package me.whereareiam.identica.type.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * Well-known provider capability identifiers.
 *
 * <p>Capabilities are advertised in {@code provider.json} as a list of ids:</p>
 * <pre>{@code
 * "capabilities": ["online", "authoritative_username"]
 * }</pre>
 */
@RequiredArgsConstructor
public enum ProviderCapability {
	/**
	 * Supports online-mode connections.
	 */
	ONLINE_MODE("online"),

	/**
	 * Supports offline-mode connections.
	 */
	OFFLINE_MODE("offline"),

	/**
	 * Publishes authoritative usernames that should be replicated.
	 */
	AUTHORITATIVE_USERNAME("authoritative_username");

	@Getter
	private final String id;

	/**
	 * Checks whether the provided id matches this capability.
	 *
	 * @param candidate capability id to compare
	 * @return {@code true} when the id matches
	 */
	public boolean matches(@Nullable String candidate) {
		if (candidate == null || candidate.isBlank()) return false;
		return id.equalsIgnoreCase(candidate.trim());
	}

	/**
	 * Resolves a capability from its id.
	 *
	 * @param id capability id
	 * @return capability or {@code null} when unknown
	 */
	public static @Nullable ProviderCapability fromId(@Nullable String id) {
		if (id == null || id.isBlank()) return null;
		for (ProviderCapability capability : values())
			if (capability.matches(id))
				return capability;

		return null;
	}
}
