package me.whereareiam.identica.provider.cracked;

import com.google.inject.Module;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.auth.AuthenticationStep;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class CrackedProvider extends IdenticaProvider {
	@Override
	public List<Module> modules() {
		return List.of(new CrackedModule());
	}

	@Override
	public List<AuthenticationStep> getAuthenticationSteps() {
		return List.of();
	}

	@Override
	public Set<String> getConflictKeys() {
		return Set.of("username");
	}

	@Override
	public Set<String> getAvailableConflictSolutions() {
		return Set.of("username_prefix", "username_suffix", "deny");
	}

	@Override
	public ConflictResolution applyConflictSolution(
			String solutionId,
			IdentityClaim claim,
			ConflictContext context
	) {
		return ConflictResolution.deny("Cracked conflict resolution not implemented");
	}
}
