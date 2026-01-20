package me.whereareiam.identica.loader;

import me.whereareiam.identica.model.provider.InternalProvider;

import java.util.List;

/**
 * Manages provider discovery and lifecycle operations.
 */
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
}
