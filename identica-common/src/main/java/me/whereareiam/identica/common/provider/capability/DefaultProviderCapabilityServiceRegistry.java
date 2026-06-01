package me.whereareiam.identica.common.provider.capability;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityServiceKey;
import me.whereareiam.identica.provider.capability.ProviderCapabilityServiceRegistry;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultProviderCapabilityServiceRegistry implements ProviderCapabilityServiceRegistry {
	private final Map<ProviderCapabilityServiceKey<?>, Object> services = new ConcurrentHashMap<>();

	@Override
	public <T> void register(@NotNull ProviderCapability capability, @NotNull Class<T> type, @NotNull T service) {
		services.put(ProviderCapabilityServiceKey.<T>builder()
				.capability(capability)
				.type(type)
				.build(), service);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> @Nullable T resolve(@NotNull ProviderCapability capability, @NotNull Class<T> type) {
		Object resolved = services.get(ProviderCapabilityServiceKey.<T>builder()
				.capability(capability)
				.type(type)
				.build());
		if (resolved == null) return null;

		return (T) resolved;
	}
}
