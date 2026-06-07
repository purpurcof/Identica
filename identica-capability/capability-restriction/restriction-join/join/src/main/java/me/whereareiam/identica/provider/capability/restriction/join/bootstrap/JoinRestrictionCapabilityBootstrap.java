package me.whereareiam.identica.provider.capability.restriction.join.bootstrap;

import com.google.inject.Injector;
import com.google.inject.Module;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDeclaration;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityGlobalInstallContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityInitializationContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityLocalInstallContext;
import me.whereareiam.identica.provider.capability.restriction.join.*;
import me.whereareiam.identica.provider.capability.restriction.join.pipeline.JoinPipelineExtension;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionSignalDescriptor;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionTypeDescriptor;
import me.whereareiam.identica.provider.capability.restriction.registry.RestrictionSignalRegistry;
import me.whereareiam.identica.provider.capability.restriction.registry.type.RestrictionTypeRegistry;
import me.whereareiam.identica.provider.capability.restriction.registry.type.RestrictionTypeResolverRegistry;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionSignal;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityKind;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Bootstrap for the typed join restriction capability.
 */
public final class JoinRestrictionCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull JoinRestrictionCapabilityBootstrap INSTANCE = new JoinRestrictionCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDeclaration declaration() {
		return ProviderCapabilityDeclaration.builder()
				.capability(JoinRestrictionCapability.CAPABILITY)
				.kind(ProviderCapabilityKind.RUNTIME)
				.scopes(Set.of(ProviderCapabilityScope.GLOBAL, ProviderCapabilityScope.LOCAL))
				.build();
	}

	@Override
	public void initialize(@NotNull ProviderCapabilityInitializationContext context) {
		Injector globalInjector = context.getGlobalInjector();
		if (globalInjector == null) return;

		globalInjector.getInstance(RestrictionTypeRegistry.class).register(RestrictionTypeDescriptor.builder()
				.type(JoinRestrictionType.TYPE)
				.displayName("Join")
				.description("Runtime restriction evaluated during connection preparation and scenario entry.")
				.build());
		globalInjector.getInstance(RestrictionSignalRegistry.class).register(RestrictionSignalDescriptor.builder()
				.restrictionType(JoinRestrictionType.TYPE)
				.signal(RestrictionSignal.of("linked"))
				.displayName("Linked")
				.description("Allows already-linked provider subjects through join restriction.")
				.build());
		globalInjector.getInstance(RestrictionTypeResolverRegistry.class)
				.register(globalInjector.getInstance(JoinRestrictionTypeResolver.class));
		globalInjector.getInstance(CommandRegistrar.class).registerCommands();

		context.getRootInjector()
				.getInstance(PipelineExtensionRegistry.class)
				.register(globalInjector.getInstance(JoinPipelineExtension.class));
	}

	@Override
	public @NotNull List<Module> globalModules(@NotNull ProviderCapabilityGlobalInstallContext context) {
		return List.of(new JoinRestrictionGlobalModule(
				context.getCapabilitiesPath()
						.resolve("restriction")
						.resolve("join")
		));
	}

	@Override
	public @NotNull List<Module> localModules(@NotNull ProviderCapabilityLocalInstallContext context) {
		return List.of(new JoinRestrictionLocalModule());
	}
}
