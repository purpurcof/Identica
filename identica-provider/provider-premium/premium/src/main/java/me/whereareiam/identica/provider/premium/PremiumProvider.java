package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.Constants;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.model.provider.dependency.ProviderLibrary;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.ProviderPlatformExtension;
import me.whereareiam.identica.provider.capability.authoritative.username.bootstrap.AuthoritativeUsernameCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.migration.bootstrap.MigrationCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.recognition.bootstrap.RecognitionCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.restriction.bootstrap.RestrictionCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.restriction.join.bootstrap.JoinRestrictionCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.verification.bootstrap.VerificationCapabilityBootstrap;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.completion.PremiumCompletionExtension;
import me.whereareiam.identica.provider.premium.completion.PremiumCompletionStep;
import me.whereareiam.identica.provider.premium.pipeline.PremiumPipelineExtension;
import me.whereareiam.identica.provider.premium.pipeline.step.shared.FinalizeProfileStep;
import me.whereareiam.identica.provider.premium.pipeline.step.shared.OfflineCheckStep;
import me.whereareiam.identica.provider.premium.pipeline.step.shared.ProfilePresenceStep;
import me.whereareiam.identica.provider.premium.pipeline.step.type.authentication.PremiumRecognitionStep;
import me.whereareiam.identica.provider.premium.pipeline.step.type.authentication.PremiumVerificationStep;
import me.whereareiam.identica.provider.premium.pipeline.step.type.migration.PremiumMigrationCompleteStep;
import me.whereareiam.identica.provider.premium.platform.bungeecord.PremiumBungeeCordExtension;
import me.whereareiam.identica.provider.premium.platform.velocity.PremiumVelocityExtension;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class PremiumProvider extends IdenticaProvider {
	private CommandRegistrar commandRegistrar;
	private PipelineExtensionRegistry pipelineExtensionRegistry;
	private CompletionExtensionRegistry completionExtensionRegistry;

	// Steps
	private ProfilePresenceStep profilePresenceStep;
	private OfflineCheckStep offlineCheckStep;
	private FinalizeProfileStep finalizeProfileStep;
	private PremiumMigrationCompleteStep premiumMigrationCompleteStep;
	private PremiumRecognitionStep premiumRecognitionStep;
	private PremiumVerificationStep premiumVerificationStep;
	private PremiumCompletionStep premiumCompletionStep;

	@Override
	public @NotNull List<Module> modules() {
		return List.of(new PremiumModule());
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
						.artifactId("authoritative-username-api")
						.version(Constants.VERSION)
						.resolveTransitiveDependencies(false)
						.loader("shared")
						.build(),
				ProviderLibrary.builder()
						.groupId("me.whereareiam.identica.capability")
						.artifactId("authoritative-username")
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
						.build()
		));
		return libraries;
	}

	@Override
	public @NotNull List<ProviderCapabilityBootstrap> capabilities() {
		return List.of(
				RestrictionCapabilityBootstrap.INSTANCE,
				JoinRestrictionCapabilityBootstrap.INSTANCE,
				RecognitionCapabilityBootstrap.INSTANCE,
				AuthoritativeUsernameCapabilityBootstrap.INSTANCE,
				MigrationCapabilityBootstrap.INSTANCE,
				VerificationCapabilityBootstrap.INSTANCE
		);
	}

	@Override
	public @NotNull List<Class<? extends ProviderPlatformExtension>> platformExtensions() {
		return List.of(
				PremiumBungeeCordExtension.class,
				PremiumVelocityExtension.class
		);
	}

	@Override
	public void onEnable() {
		commandRegistrar.registerCommands();
		pipelineExtensionRegistry.register(new PremiumPipelineExtension(
				descriptor.getId(),
				profilePresenceStep,
				offlineCheckStep,
				finalizeProfileStep,
				premiumMigrationCompleteStep,
				premiumRecognitionStep,
				premiumVerificationStep
		));
		completionExtensionRegistry.register(new PremiumCompletionExtension(
				descriptor.getId(),
				premiumCompletionStep
		));
		if (platformExtension != null)
			platformExtension.onEnable();
	}

	@Override
	public void onDisable() {
		if (platformExtension != null) platformExtension.onDisable();
		pipelineExtensionRegistry.unregister(PremiumPipelineExtension.extensionIdFor(descriptor.getId()));
		completionExtensionRegistry.unregister(PremiumCompletionExtension.extensionIdFor(descriptor.getId()));
	}
}
