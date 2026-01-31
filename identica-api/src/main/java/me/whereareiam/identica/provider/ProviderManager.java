package me.whereareiam.identica.provider;

import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.resolver.ProviderResolver;
import me.whereareiam.identica.type.provider.ProviderCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Manages provider discovery and lifecycle operations.
 */
@SuppressWarnings("unused")
public interface ProviderManager {
	/**
	 * Discovers and loads enabled providers.
	 */
	void loadProviders();

	/**
	 * Disables and unloads all loaded providers.
	 */
	void unloadProviders();

	/**
	 * Returns the currently managed providers.
	 *
	 * @return immutable view of managed providers
	 */
	List<InternalProvider> getProviders();

	/**
	 * Resolves profile subject data using registered provider resolvers.
	 *
	 * @param context profile resolve context
	 * @return resolution or {@code null} when no resolver applies
	 */
	@Nullable ProfileResolution resolveProfile(@NotNull ProfileResolveContext context);

	/**
	 * Finds providers that advertise all requested capabilities.
	 *
	 * @param capabilities required capabilities
	 * @return immutable list of matching providers ordered by priority
	 */
	@NotNull List<InternalProvider> findProviders(@NotNull ProviderCapability... capabilities);

	/**
	 * Finds the highest priority provider that advertises all requested capabilities.
	 *
	 * @param capabilities required capabilities
	 * @return matching provider or {@code null} when none match
	 */
	@Nullable InternalProvider findProvider(@NotNull ProviderCapability... capabilities);

	/**
	 * Registers a resolver used during provider loading.
	 */
	void registerResolver(ProviderResolver resolver);

	/**
	 * Removes a resolver used during provider loading.
	 */
	void unregisterResolver(ProviderResolver resolver);

	/**
	 * Returns the currently registered resolvers.
	 *
	 * @return immutable view of resolvers
	 */
	List<ProviderResolver> getResolvers();
}
