package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.auth.step.registry.StepRegistry;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.platform.velocity.PremiumVelocityModule;
import me.whereareiam.identica.provider.premium.platform.velocity.listener.connection.PremiumGameProfileRequestListener;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileSubjectResolver;
import me.whereareiam.identica.provider.premium.step.VerifyPremiumProfileStep;
import me.whereareiam.identica.registry.ProfileSubjectResolverRegistry;
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
	private ProfileSubjectResolverRegistry profileResolverRegistry;

	private PremiumHandshakePolicy premiumHandshakePolicy;
	private PremiumEligibilityResolver premiumEligibilityResolver;

	private CommandRegistrar commandRegistrar;
	private StepRegistry stepRegistry;
	private DynamicListenerRegistry listenerRegistry;
	private PremiumGameProfileRequestListener gameProfileRequestListener;

	// Steps
	private VerifyPremiumProfileStep verifyPremiumProfileStep;
	private PremiumProfileSubjectResolver profileSubjectResolver;

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
		eligibilityResolvers.register(premiumEligibilityResolver);
		handshakePolicies.register(premiumHandshakePolicy);
		profileResolverRegistry.register(descriptor.getId(), profileSubjectResolver);
	}

	@Override
	public void onDisable() {
		handshakePolicies.unregister(premiumHandshakePolicy);
		eligibilityResolvers.unregister(premiumEligibilityResolver);
		profileResolverRegistry.unregister(descriptor.getId(), profileSubjectResolver);
	}
}
