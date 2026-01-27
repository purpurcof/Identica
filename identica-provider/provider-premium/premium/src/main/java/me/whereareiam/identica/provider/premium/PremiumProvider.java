package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakePolicy;
import me.whereareiam.identica.provider.premium.step.PremiumIntentStep;
import me.whereareiam.identica.provider.premium.step.VerifyPremiumProfileStep;
import me.whereareiam.identica.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@SuppressWarnings("unused")
@AllArgsConstructor(onConstructor_ = @Inject)
public class PremiumProvider extends IdenticaProvider {
	private Injector injector;

	private CommandRegistrar commandRegistrar;
	private Registry<HandshakePolicy> handshakePolicies;
	private PremiumHandshakePolicy premiumHandshakePolicy;

	@Override
	public @NotNull List<Module> modules() {
		return List.of(
				new PremiumModule()
		);
	}

	@Override
	public @NotNull List<AuthenticationStep> getAuthenticationSteps() {
		return List.of(
				injector.getInstance(PremiumIntentStep.class),
				injector.getInstance(VerifyPremiumProfileStep.class)
		);
	}

	@Override
	public void onEnable() {
		commandRegistrar.registerCommands();
		handshakePolicies.register(premiumHandshakePolicy);
	}

	@Override
	public void onDisable() {
		handshakePolicies.unregister(premiumHandshakePolicy);
	}

}
