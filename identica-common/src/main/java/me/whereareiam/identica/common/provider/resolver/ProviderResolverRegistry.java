package me.whereareiam.identica.common.provider.resolver;

import com.google.inject.Singleton;
import me.whereareiam.identica.provider.resolver.ProviderResolver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class ProviderResolverRegistry {
	private final List<ProviderResolver> resolvers = new CopyOnWriteArrayList<>();

	public void register(ProviderResolver resolver) {
		if (resolver == null) return;
		if (!resolvers.contains(resolver)) {
			resolvers.add(resolver);
		}
	}

	public void unregister(ProviderResolver resolver) {
		if (resolver == null) return;
		resolvers.remove(resolver);
	}

	public List<ProviderResolver> getAll() {
		return Collections.unmodifiableList(resolvers);
	}
}
