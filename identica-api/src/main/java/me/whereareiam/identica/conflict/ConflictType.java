package me.whereareiam.identica.conflict;

import me.whereareiam.identica.conflict.resolver.ConflictResolver;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.type.ConflictHook;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Describes a conflict that can occur and how it should be handled.
 */
public interface ConflictType {
	/**
	 * Conflict key used in configuration and context.
	 *
	 * @return conflict key
	 */
	@NotNull
	String getKey();

	/**
	 * Default hook location for this conflict type.
	 *
	 * @return default hook
	 */
	@NotNull
	default ConflictHook getDefaultHook() {
		return ConflictHook.NONE;
	}

	/**
	 * Return resolvers owned by this conflict type.
	 *
	 * <pre>{@code
	 * @Override
	 * public List<ConflictResolver> getResolvers() {
	 *     return List.of(formatResolver);
	 * }
	 * }</pre>
	 *
	 * @return resolvers owned by the type
	 */
	default @NotNull List<ConflictResolver> getResolvers() {
		return List.of();
	}

	/**
	 * Return guards owned by this conflict type.
	 *
	 * <pre>{@code
	 * @Override
	 * public List<ConflictGuard> getGuards() {
	 *     return List.of(entrypointGuard);
	 * }
	 * }</pre>
	 *
	 * @return guards owned by the type
	 */
	default @NotNull List<ConflictGuard> getGuards() {
		return List.of();
	}

	/**
	 * Build a conflict context during account preparation.
	 *
	 * <pre>{@code
	 * ConflictContext context = type.createContext(event);
	 * if (context != null) {
	 *     ConflictResolution resolution = conflictService.resolve(context);
	 *     if (resolution != null) type.apply(event, context, resolution);
	 * }
	 * }</pre>
	 *
	 * @param event account prepare event
	 * @return conflict context or {@code null} if no conflict applies
	 */
	@Nullable
	ConflictContext createContext(@NotNull AccountPrepareEvent event);

	/**
	 * Apply the resolved conflict decision to the event.
	 *
	 * @param event account prepare event
	 * @param context conflict context
	 * @param resolution conflict resolution
	 */
	void apply(
			@NotNull AccountPrepareEvent event,
			@NotNull ConflictContext context,
			@NotNull ConflictResolution resolution
	);
}
