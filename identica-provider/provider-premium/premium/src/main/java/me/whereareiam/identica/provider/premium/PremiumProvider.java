package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.auth.step.registry.StepRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.step.VerifyPremiumProfileStep;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class PremiumProvider extends IdenticaProvider {
	private Registry<HandshakePolicy> handshakePolicies;
	private Registry<ProviderEligibilityResolver> eligibilityResolvers;

	private PremiumHandshakePolicy premiumHandshakePolicy;
	private PremiumEligibilityResolver premiumEligibilityResolver;

	private CommandRegistrar commandRegistrar;
	private StepRegistry stepRegistry;

	// Steps
	private VerifyPremiumProfileStep verifyPremiumProfileStep;

	@Override
	public @NotNull List<Module> modules() {
		return List.of(
				new PremiumModule()
		);
	}

	@Override
	public void onEnable() {
		commandRegistrar.registerCommands();
		stepRegistry.register(descriptor.getId(), StepPhase.PROVIDER, 10, verifyPremiumProfileStep);
		eligibilityResolvers.register(premiumEligibilityResolver);
		handshakePolicies.register(premiumHandshakePolicy);
	}

	@Override
	public void onDisable() {
		handshakePolicies.unregister(premiumHandshakePolicy);
		eligibilityResolvers.unregister(premiumEligibilityResolver);
	}

}
