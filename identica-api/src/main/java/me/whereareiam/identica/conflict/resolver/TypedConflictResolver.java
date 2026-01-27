package me.whereareiam.identica.conflict.resolver;

import me.whereareiam.configura.node.Node;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.util.ConflictParameters;
import org.jetbrains.annotations.NotNull;

/**
 * Conflict resolver that binds configuration parameters to a typed config object.
 *
 * @param <T> config type
 */
public interface TypedConflictResolver<T> extends ConflictResolver {
	/**
	 * Configuration type used for binding resolver parameters.
	 *
	 * @return config class
	 */
	@NotNull Class<T> getConfigType();

	/**
	 * Resolve a conflict using typed configuration.
	 *
	 * @param context conflict context
	 * @param config resolver configuration
	 * @return conflict resolution
	 */
	@NotNull ConflictResolution resolve(
			@NotNull ConflictContext context,
			@NotNull T config
	);

	@Override
	default @NotNull ConflictResolution resolve(
			@NotNull ConflictContext context,
			@NotNull Node params
	) {
		T config = ConflictParameters.bind(params, getConfigType());
		return resolve(context, config);
	}
}
