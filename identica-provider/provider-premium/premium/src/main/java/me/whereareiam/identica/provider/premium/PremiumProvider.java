package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.auth.step.registry.StepRegistry;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.platform.velocity.PremiumVelocityModule;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;
import me.whereareiam.identica.provider.premium.step.VerifyPremiumProfileStep;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class PremiumProvider extends IdenticaProvider {
	private CommandRegistrar commandRegistrar;
	private StepRegistry stepRegistry;
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
		stepRegistry.register(descriptor.getId(), StepPhase.PROVIDER, 10, verifyPremiumProfileStep);
		listenerRegistry.register(gameProfileRequestListener);
	}

	@Override
	public void onDisable() {
	}
}
