package me.whereareiam.identica.provider.cracked;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.BuildConfig;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.model.provider.dependency.ProviderLibrary;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.cracked.command.CommandRegistrar;
import me.whereareiam.identica.provider.cracked.completion.CrackedCompletionExtension;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyModule;
import me.whereareiam.identica.provider.cracked.cryptography.argon2.Argon2CryptographyModule;
import me.whereareiam.identica.provider.cracked.cryptography.bcrypt.BcryptCryptographyModule;
import me.whereareiam.identica.provider.cracked.database.DatabaseModule;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedPipelineExtension;
import me.whereareiam.identica.provider.cracked.sentinel.BruteForceSentinelDefinition;
import me.whereareiam.identica.sentinel.SentinelDefinition;
import me.whereareiam.identica.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class CrackedProvider extends IdenticaProvider {
	private CommandRegistrar commandRegistrar;
	private PipelineExtensionRegistry pipelineExtensionRegistry;
	private CompletionExtensionRegistry completionExtensionRegistry;
	private CrackedPipelineExtension crackedPipelineExtension;
	private CrackedCompletionExtension crackedCompletionExtension;
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
	public @NotNull ProviderLibraries libraries() {
		ProviderLibraries libraries = new ProviderLibraries();
		libraries.setLibraries(List.of(
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
		pipelineExtensionRegistry.register(crackedPipelineExtension);
		completionExtensionRegistry.register(crackedCompletionExtension);
		sentinelRegistry.register(bruteForceSentinelDefinition);
	}

	@Override
	public void onDisable() {
		pipelineExtensionRegistry.unregister(CrackedPipelineExtension.extensionId());
		completionExtensionRegistry.unregister(CrackedCompletionExtension.extensionId());
		sentinelRegistry.unregister(bruteForceSentinelDefinition);
	}
}
