package me.whereareiam.identica.common.provider.capability;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityInstallation;
import me.whereareiam.identica.provider.capability.ProviderCapabilityRegistry;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultProviderCapabilityRegistry implements ProviderCapabilityRegistry {
	private final Map<ProviderCapability, ProviderCapabilityInstallation> installations = new ConcurrentHashMap<>();

	@Override
	public @Nullable ProviderCapabilityInstallation findInstallation(@NotNull ProviderCapability capability) {
		return installations.get(capability);
	}

	@Override
	public @NotNull ProviderCapabilityInstallation registerInstallation(@NotNull ProviderCapabilityInstallation installation) {
		installations.put(installation.getBootstrap().descriptor().getCapability(), installation);
		return installation;
	}

	@Override
	public boolean unregisterInstallation(@NotNull ProviderCapability capability) {
		return installations.remove(capability) != null;
	}

	@Override
	public @NotNull List<ProviderCapabilityInstallation> installations() {
		return List.copyOf(installations.values());
	}
}
