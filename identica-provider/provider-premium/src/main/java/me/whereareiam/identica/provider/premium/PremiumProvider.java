package me.whereareiam.identica.provider.premium;

import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.auth.AuthenticationStep;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class PremiumProvider extends IdenticaProvider {
	@Override
	public List<AuthenticationStep> getAuthenticationSteps() {
		return List.of();
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
		return ConflictResolution.deny("Premium account conflict");
	}
}
