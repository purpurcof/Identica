package me.whereareiam.identica.provider.subject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a provider subject for a connection identity.
 */
public interface SubjectResolver {
	/**
	 * Returns whether this resolver can handle the supplied context.
	 *
	 * @param context subject resolution context
	 * @return {@code true} when this resolver can resolve the subject
	 */
	boolean supports(@NotNull SubjectResolveContext context);

	/**
	 * Resolves provider-subject data for the supplied context.
	 *
	 * @param context subject resolution context
	 * @return resolved subject data, or {@code null} when unavailable
	 */
	@Nullable SubjectResolution resolve(@NotNull SubjectResolveContext context);

	/**
	 * Returns resolver priority, where higher values win first.
	 *
	 * @return resolver priority
	 */
	default int priority() {
		return 0;
	}
}
