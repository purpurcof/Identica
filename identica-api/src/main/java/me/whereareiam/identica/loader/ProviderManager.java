package me.whereareiam.identica.loader;

import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.loader.resolver.ProviderResolver;

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
