package me.whereareiam.identica.common.loader.resolver;

import me.whereareiam.identica.model.provider.InternalProvider;

public interface ProviderResolver {
	boolean resolve(InternalProvider provider);
}
