package me.whereareiam.identica.provider.credential;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.BuildConfig;
import me.whereareiam.identica.Constants;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.model.provider.dependency.ProviderLibrary;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.migration.bootstrap.MigrationCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.offline.bootstrap.OfflineCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.recognition.bootstrap.RecognitionCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.restriction.bootstrap.RestrictionCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.restriction.join.bootstrap.JoinRestrictionCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.verification.bootstrap.VerificationCapabilityBootstrap;
import me.whereareiam.identica.provider.credential.command.CommandRegistrar;
import me.whereareiam.identica.provider.credential.completion.CredentialCompletionExtension;
import me.whereareiam.identica.provider.credential.cryptography.CryptographyModule;
import me.whereareiam.identica.provider.credential.cryptography.argon2.Argon2CryptographyModule;
import me.whereareiam.identica.provider.credential.cryptography.bcrypt.BcryptCryptographyModule;
import me.whereareiam.identica.provider.credential.database.DatabaseModule;
import me.whereareiam.identica.provider.credential.pipeline.CredentialPipelineExtension;
import me.whereareiam.identica.provider.credential.sentinel.BruteForceSentinelDefinition;
import me.whereareiam.identica.sentinel.SentinelDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class CredentialProvider extends IdenticaProvider {
	private CommandRegistrar commandRegistrar;
	private PipelineExtensionRegistry pipelineExtensionRegistry;
	private CompletionExtensionRegistry completionExtensionRegistry;
	private CredentialPipelineExtension credentialPipelineExtension;
	private CredentialCompletionExtension credentialCompletionExtension;
	private Registry<SentinelDefinition> sentinelRegistry;
	private BruteForceSentinelDefinition bruteForceSentinelDefinition;

	@Override
	public @NotNull List<Module> modules() {
		return List.of(
				new CommonConfiguration(),
				new DatabaseModule(),
				new CryptographyModule(),
				new BcryptCryptographyModule(),
				new Argon2CryptographyModule()
		);
	}

	@Override
	public @NotNull List<ProviderCapabilityBootstrap> capabilities() {
		return List.of(
				RestrictionCapabilityBootstrap.INSTANCE,
				JoinRestrictionCapabilityBootstrap.INSTANCE,
				RecognitionCapabilityBootstrap.INSTANCE,
				OfflineCapabilityBootstrap.INSTANCE,
				MigrationCapabilityBootstrap.INSTANCE,
				VerificationCapabilityBootstrap.INSTANCE
		);
	}

	@Override
	public @NotNull ProviderLibraries libraries() {
		ProviderLibraries libraries = new ProviderLibraries();
		libraries.setLibraries(List.of(
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("restriction-api")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.loader("shared")
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("restriction")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("restriction-join-api")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.loader("shared")
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("restriction-join")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("recognition-api")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.loader("shared")
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("recognition")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("offline-api")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.loader("shared")
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("offline")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("migration-api")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.loader("shared")
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("migration")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("verification-api")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.loader("shared")
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("verification")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.build(),
				ProviderLibrary.builder()
						.groupId("at.favre.lib")
						.artifactId("bcrypt")
						.version(BuildConfig.BCRYPT)
						.build(),
				ProviderLibrary.builder()
						.groupId("de.mkammerer")
						.artifactId("argon2-jvm")
						.version(BuildConfig.ARGON2)
						.build()
		));
		return libraries;
	}

	@Override
	public void onEnable() {
		commandRegistrar.registerCommands();
		pipelineExtensionRegistry.register(credentialPipelineExtension);
		completionExtensionRegistry.register(credentialCompletionExtension);
		sentinelRegistry.register(bruteForceSentinelDefinition);
	}

	@Override
	public void onDisable() {
		pipelineExtensionRegistry.unregister(CredentialPipelineExtension.extensionId());
		completionExtensionRegistry.unregister(CredentialCompletionExtension.extensionId());
		sentinelRegistry.unregister(bruteForceSentinelDefinition);
	}
}
