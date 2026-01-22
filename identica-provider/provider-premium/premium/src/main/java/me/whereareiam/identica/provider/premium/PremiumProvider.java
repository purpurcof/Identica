package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.provider.premium.database.PremiumDatabaseConfiguration;
import me.whereareiam.identica.provider.premium.command.CommandRegistrar;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakePolicy;
import me.whereareiam.identica.provider.premium.step.PremiumIntentStep;
import me.whereareiam.identica.provider.premium.step.VerifyPremiumProfileStep;
import me.whereareiam.identica.registry.Registry;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @Inject)
@SuppressWarnings("unused")
public class PremiumProvider extends IdenticaProvider {
	private Injector injector;

	private CommandRegistrar commandRegistrar;
	private Registry<HandshakePolicy> handshakePolicies;
	private PremiumHandshakePolicy premiumHandshakePolicy;
	private PremiumListenerRegistrar listenerRegistrar;

	@Override
	public List<Module> modules() {
		return List.of(
				new PremiumDatabaseConfiguration(),
				new PremiumModule()
		);
	}

	@Override
	public List<AuthenticationStep> getAuthenticationSteps() {
		return List.of(
				injector.getInstance(PremiumIntentStep.class),
				injector.getInstance(VerifyPremiumProfileStep.class)
		);
	}

	@Override
	public void onEnable() {
		commandRegistrar.registerCommands();
		handshakePolicies.register(premiumHandshakePolicy);
		listenerRegistrar.register();
	}

	@Override
	public void onDisable() {
		handshakePolicies.unregister(premiumHandshakePolicy);
		listenerRegistrar.unregister();
	}

	@Override
	public Set<String> getConflictKeys() {
		return Set.of("username", "mojangUniqueId");
	}

	@Override
	public Set<String> getAvailableConflictSolutions() {
		return Set.of("deny");
	}

	@Override
	public ConflictResolution applyConflictSolution(
			String solutionId,
			IdentityClaim claim,
			ConflictContext context
	) {
		return ConflictResolution.allow(); // TODO
	}
}
