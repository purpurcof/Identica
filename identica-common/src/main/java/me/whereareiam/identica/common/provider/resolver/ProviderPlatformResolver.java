package me.whereareiam.identica.common.provider.resolver;

import com.google.inject.Singleton;
import me.whereareiam.identica.provider.resolver.ProviderResolver;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.PlatformType;

import java.util.List;

@Singleton
public class ProviderPlatformResolver implements ProviderResolver {
	private final PlatformType platformType = PlatformType.getType();

	@Override
	public boolean resolve(InternalProvider provider) {
		ProviderDescriptor descriptor = provider.getDescriptor();
		List<String> supportedPlatforms = descriptor != null ? descriptor.getSupportedPlatforms() : null;

		if (supportedPlatforms == null || supportedPlatforms.isEmpty())
			return true;

		boolean supported = supportedPlatforms.stream()
				.filter(entry -> entry != null && !entry.isBlank())
				.anyMatch(entry -> entry.trim().equalsIgnoreCase(platformType.name())
						|| entry.trim().equalsIgnoreCase("any")
				);

		if (!supported) Logger.warn("Provider %s does not support platform %s", descriptor.getId(), platformType);

		return supported;
	}
}
