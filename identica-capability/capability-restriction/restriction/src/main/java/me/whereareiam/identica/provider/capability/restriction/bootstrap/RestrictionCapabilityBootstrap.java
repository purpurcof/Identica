package me.whereareiam.identica.provider.capability.restriction.bootstrap;

import com.google.inject.Injector;
import com.google.inject.Module;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDeclaration;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityGlobalInstallContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityInitializationContext;
import me.whereareiam.identica.provider.capability.restriction.RestrictionCapability;
import me.whereareiam.identica.provider.capability.restriction.RestrictionGlobalModule;
import me.whereareiam.identica.provider.capability.restriction.config.RestrictionSettings;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Bootstrap for the generic restriction capability runtime.
 */
public final class RestrictionCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull RestrictionCapabilityBootstrap INSTANCE = new RestrictionCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDeclaration declaration() {
		return ProviderCapabilityDeclaration.builder()
				.capability(RestrictionCapability.CAPABILITY)
				.scopes(Set.of(ProviderCapabilityScope.GLOBAL))
				.build();
	}

	@Override
	public void initialize(@NotNull ProviderCapabilityInitializationContext context) {
		Injector globalInjector = context.getGlobalInjector();
		if (globalInjector == null) return;

		globalInjector.getInstance(RestrictionSettings.class);
	}

	@Override
	public @NotNull List<Module> globalModules(@NotNull ProviderCapabilityGlobalInstallContext context) {
		return List.of(new RestrictionGlobalModule(
				context.getCapabilitiesPath().resolve(RestrictionCapability.CAPABILITY.getId())
		));
	}
}
