package me.whereareiam.identica.provider.cracked;

import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrackedProvider extends IdenticaProvider {
	private final Provider<CrackedMessages> messagesProvider;

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
		CrackedMessages.Conflict conflict = messagesProvider.get().getConflict();
		String denied = conflict != null && conflict.getDenied() != null
				? String.join("\n", conflict.getDenied())
				: null;
		return ConflictResolution.deny(denied);
	}
}
