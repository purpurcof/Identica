package me.whereareiam.identica.provider.capability.authoritative.username.bootstrap;

import com.google.inject.Injector;
import com.google.inject.Module;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.database.schema.SchemaBootstrap;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDeclaration;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.capability.authoritative.username.AuthoritativeUsernameGlobalModule;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.UsernameConflictType;
import me.whereareiam.identica.provider.capability.authoritative.username.database.AuthoritativeUsernameDatabaseModule;
import me.whereareiam.identica.provider.capability.authoritative.username.database.AuthoritativeUsernameSchemaContributor;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.AuthoritativeUsernamePipelineExtension;
import me.whereareiam.identica.provider.capability.authoritative.username.type.AuthoritativeUsernameCapability;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityGlobalInstallContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityInitializationContext;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityKind;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Bootstrap for the built-in authoritative username capability.
 */
public final class AuthoritativeUsernameCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull AuthoritativeUsernameCapabilityBootstrap INSTANCE =
			new AuthoritativeUsernameCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDeclaration declaration() {
		return ProviderCapabilityDeclaration.builder()
				.capability(AuthoritativeUsernameCapability.CAPABILITY)
				.kind(ProviderCapabilityKind.RUNTIME)
				.scopes(Set.of(ProviderCapabilityScope.GLOBAL, ProviderCapabilityScope.LOCAL))
				.build();
	}

	@Override
	public void initialize(@NotNull ProviderCapabilityInitializationContext context) {
		Injector globalInjector = context.getGlobalInjector();
		if (globalInjector == null) return;

		context.getRootInjector()
				.getInstance(PipelineExtensionRegistry.class)
				.register(globalInjector.getInstance(AuthoritativeUsernamePipelineExtension.class));
		context.getRootInjector()
				.getInstance(ConflictService.class)
				.register(globalInjector.getInstance(UsernameConflictType.class));
		context.getRootInjector()
				.getInstance(SchemaBootstrap.class)
				.apply(globalInjector.getInstance(AuthoritativeUsernameSchemaContributor.class));
	}

	@Override
	public @NotNull List<Module> globalModules(@NotNull ProviderCapabilityGlobalInstallContext context) {
		return List.of(
				new AuthoritativeUsernameGlobalModule(
						context.getCapabilitiesPath().resolve(AuthoritativeUsernameCapability.CAPABILITY.getId())
				),
				new AuthoritativeUsernameDatabaseModule()
		);
	}
}
