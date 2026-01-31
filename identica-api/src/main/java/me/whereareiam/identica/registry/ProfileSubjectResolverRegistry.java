package me.whereareiam.identica.registry;

import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registry for profile subject resolvers grouped by provider id.
 */
public interface ProfileSubjectResolverRegistry {
	/**
	 * Registers a resolver for the given provider id.
	 *
	 * @param providerId provider id owning the resolver
	 * @param resolver resolver to register
	 */
	void register(@NotNull String providerId, @NotNull ProfileSubjectResolver resolver);

	/**
	 * Removes a resolver previously registered for the given provider id.
	 *
	 * @param providerId provider id owning the resolver
	 * @param resolver resolver to unregister
	 */
	void unregister(@NotNull String providerId, @NotNull ProfileSubjectResolver resolver);

	/**
	 * Returns resolvers registered for the given provider id.
	 *
	 * @param providerId provider id to lookup
	 * @return list of resolvers (may be empty)
	 */
	@NotNull List<ProfileSubjectResolver> getResolvers(@NotNull String providerId);
}
