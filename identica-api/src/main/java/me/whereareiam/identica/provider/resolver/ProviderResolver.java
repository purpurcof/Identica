package me.whereareiam.identica.provider.resolver;

import me.whereareiam.identica.model.provider.InternalProvider;

public interface ProviderResolver {
	boolean resolve(InternalProvider provider);
}
