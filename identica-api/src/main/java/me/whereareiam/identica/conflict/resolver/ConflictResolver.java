package me.whereareiam.identica.conflict.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves conflicts for a specific resolver ID.
 */
public interface ConflictResolver {
	/**
	 * Resolver id used in configuration.
	 *
	 * @return resolver id
	 */
	@NotNull String getId();

	/**
	 * Returns {@code true} if this resolver can handle the provided key.
	 *
	 * @param key conflict key
	 * @return whether this resolver supports the key
	 */
	default boolean supports(@NotNull String key) {
		return true;
	}

	/**
	 * Resolve a conflict.
	 *
	 * @param context conflict context
	 * @param params configuration parameters
	 * @return conflict resolution
	 */
	@NotNull ConflictResolution resolve(
			@NotNull ConflictContext context,
			@NotNull JsonNode params
	);
}
