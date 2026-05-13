package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.completion.PremiumCompletionExtension;
import me.whereareiam.identica.provider.premium.completion.PremiumCompletionStep;
import me.whereareiam.identica.provider.premium.pipeline.PremiumPipelineExtension;
import me.whereareiam.identica.provider.premium.platform.bungeecord.PremiumBungeeCordModule;
import me.whereareiam.identica.provider.premium.platform.bungeecord.listener.connection.PremiumPostLoginListener;
import me.whereareiam.identica.provider.premium.platform.velocity.PremiumVelocityModule;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;
import me.whereareiam.identica.provider.premium.step.*;
import me.whereareiam.identica.type.PlatformType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class PremiumProvider extends IdenticaProvider {
	private CommandRegistrar commandRegistrar;
	private PipelineExtensionRegistry pipelineExtensionRegistry;
	private CompletionExtensionRegistry completionExtensionRegistry;
	private DynamicListenerRegistry listenerRegistry;
	private @Nullable PremiumGameProfileRequestListener gameProfileRequestListener;
	private @Nullable PremiumPostLoginListener postLoginListener;

	// Steps
	private ProfilePresenceStep profilePresenceStep;
	private OfflineCheckStep offlineCheckStep;
	private FinalizeProfileStep finalizeProfileStep;
	private PremiumMigrationCompleteStep premiumMigrationCompleteStep;
	private PremiumVerificationStep premiumVerificationStep;
	private PremiumCompletionStep premiumCompletionStep;

	@Override
	public @NotNull List<Module> modules() {
		List<Module> modules = new ArrayList<>();
		modules.add(new PremiumModule());

		PlatformType platformType = PlatformType.getType();
		if (platformType == PlatformType.BUNGEECORD)
			modules.add(new PremiumBungeeCordModule());
		if (platformType == PlatformType.VELOCITY)
			modules.add(new PremiumVelocityModule());

		return List.copyOf(modules);
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
				premiumVerificationStep
		));
		completionExtensionRegistry.register(new PremiumCompletionExtension(
				descriptor.getId(),
				premiumCompletionStep
		));
		registerPlatformListeners();
	}

	@Override
	public void onDisable() {
		pipelineExtensionRegistry.unregister(PremiumPipelineExtension.extensionIdFor(descriptor.getId()));
		completionExtensionRegistry.unregister(PremiumCompletionExtension.extensionIdFor(descriptor.getId()));
	}

	private void registerPlatformListeners() {
		PlatformType platformType = PlatformType.getType();
		if (platformType == PlatformType.VELOCITY && gameProfileRequestListener != null) {
			listenerRegistry.register(gameProfileRequestListener);
			return;
		}

		if (platformType == PlatformType.BUNGEECORD && postLoginListener != null)
			listenerRegistry.register(postLoginListener);
	}
}
