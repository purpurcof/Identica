package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.completion.PremiumCompletionExtension;
import me.whereareiam.identica.provider.premium.completion.PremiumCompletionStep;
import me.whereareiam.identica.provider.premium.pipeline.PremiumPipelineExtension;
import me.whereareiam.identica.provider.premium.platform.velocity.PremiumVelocityModule;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;
import me.whereareiam.identica.provider.premium.step.FinalizeProfileStep;
import me.whereareiam.identica.provider.premium.step.OfflineCheckStep;
import me.whereareiam.identica.provider.premium.step.PremiumVerificationStep;
import me.whereareiam.identica.provider.premium.step.ProfilePresenceStep;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class PremiumProvider extends IdenticaProvider {
	private CommandRegistrar commandRegistrar;
	private PipelineExtensionRegistry pipelineExtensionRegistry;
	private CompletionExtensionRegistry completionExtensionRegistry;
	private DynamicListenerRegistry listenerRegistry;
	private PremiumGameProfileRequestListener gameProfileRequestListener;

	// Steps
	private ProfilePresenceStep profilePresenceStep;
	private OfflineCheckStep offlineCheckStep;
	private FinalizeProfileStep finalizeProfileStep;
	private PremiumVerificationStep premiumVerificationStep;
	private PremiumCompletionStep premiumCompletionStep;

	@Override
	public @NotNull List<Module> modules() {
		return List.of(
				new PremiumModule(),
				new PremiumVelocityModule()
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
				premiumVerificationStep
		));
		completionExtensionRegistry.register(new PremiumCompletionExtension(
				descriptor.getId(),
				premiumCompletionStep
		));
		listenerRegistry.register(gameProfileRequestListener);
	}

	@Override
	public void onDisable() {
		pipelineExtensionRegistry.unregister(PremiumPipelineExtension.extensionIdFor(descriptor.getId()));
		completionExtensionRegistry.unregister(PremiumCompletionExtension.extensionIdFor(descriptor.getId()));
	}
}
