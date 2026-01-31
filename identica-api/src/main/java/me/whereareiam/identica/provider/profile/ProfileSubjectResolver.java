package me.whereareiam.identica.provider.profile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolver used to derive provider subject data during profile rewrite.
 */
@SuppressWarnings("unused")
public interface ProfileSubjectResolver {
	/**
	 * Returns whether this resolver can handle the provided context.
	 *
	 * @param context profile resolve context
	 * @return {@code true} when this resolver supports the request
	 */
	boolean supports(@NotNull ProfileResolveContext context);

	/**
	 * Resolves provider subject information for the provided context.
	 *
	 * @param context profile resolve context
	 * @return resolved profile data or {@code null} when no resolution applies
	 */
	@Nullable ProfileResolution resolve(@NotNull ProfileResolveContext context);

	/**
	 * Priority used to order resolvers. Higher values are preferred.
	 *
	 * @return resolver priority
	 */
	default int priority() {
		return 0;
	}
}
