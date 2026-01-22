package me.whereareiam.identica.loader.resolver;

import me.whereareiam.identica.model.provider.InternalProvider;

public interface ProviderResolver {
	boolean resolve(InternalProvider provider);
}
