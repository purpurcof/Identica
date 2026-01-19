package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.model.provider.ProviderLibraries;

public interface ProviderDependencyResolver {
	void loadLibraries(String providerId, ProviderLibraries libraries, ClassLoader classLoader);
}
