package me.whereareiam.identica.conflict;

import me.whereareiam.identica.conflict.resolver.ConflictResolver;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Resolves conflicts using configured rules and registered resolvers.
 */
public interface ConflictService {
	/**
	 * Register a resolver for use in conflict resolution.
	 *
	 * @param resolver resolver to register
	 */
	void register(@NotNull ConflictResolver resolver);

	/**
	 * Unregister a resolver.
	 *
	 * @param resolver resolver to unregister
	 */
	void unregister(@NotNull ConflictResolver resolver);

	/**
	 * Resolve a registered resolver by id.
	 *
	 * @param id resolver id
	 * @return resolver or {@code null}
	 */
	@Nullable
	ConflictResolver getResolver(@NotNull String id);

	/**
	 * Register a conflict type.
	 *
	 * @param type conflict type
	 */
	void register(@NotNull ConflictType<?> type);

	/**
	 * Unregister a conflict type.
	 *
	 * @param type conflict type
	 */
	void unregister(@NotNull ConflictType<?> type);

	/**
	 * Resolve a registered conflict type by key.
	 *
	 * @param key conflict key
	 * @return conflict type or {@code null}
	 */
	@Nullable
	ConflictType<?> getType(@NotNull String key);

	/**
	 * Return registered conflict types.
	 *
	 * @return conflict types
	 */
	@NotNull
	Set<ConflictType<?>> getTypes();

	/**
	 * Resolve a conflict for the provided context.
	 *
	 * <pre>{@code
	 * ConflictResolution resolution = conflictService.resolve(context);
	 * if (resolution != null && resolution.getDecision() == ConflictResolution.Decision.DENY) {
	 *     // handle denial
	 * }
	 * }</pre>
	 *
	 * @param context conflict context
	 * @return conflict resolution or {@code null} when no resolver applies
	 */
	@Nullable
	ConflictResolution resolve(@NotNull ConflictContext context);
}
