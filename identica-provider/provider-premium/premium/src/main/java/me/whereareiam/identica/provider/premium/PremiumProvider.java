package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.pipeline.PremiumVerifyPipelineExtension;
import me.whereareiam.identica.provider.premium.platform.velocity.PremiumVelocityModule;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;
import me.whereareiam.identica.provider.premium.step.VerifyPremiumProfileStep;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class PremiumProvider extends IdenticaProvider {
	private CommandRegistrar commandRegistrar;
	private PipelineExtensionRegistry pipelineExtensionRegistry;
	private DynamicListenerRegistry listenerRegistry;
	private PremiumGameProfileRequestListener gameProfileRequestListener;

	// Steps
	private VerifyPremiumProfileStep verifyPremiumProfileStep;

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
		pipelineExtensionRegistry.register(new PremiumVerifyPipelineExtension(descriptor.getId(), verifyPremiumProfileStep));
		listenerRegistry.register(gameProfileRequestListener);
	}

	@Override
	public void onDisable() {
		pipelineExtensionRegistry.unregister(descriptor.getId());
	}
}
