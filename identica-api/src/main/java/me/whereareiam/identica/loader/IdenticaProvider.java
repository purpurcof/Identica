package me.whereareiam.identica.loader;

import lombok.Setter;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.google.inject.Module;

@Setter
@SuppressWarnings("unused")
public abstract class IdenticaProvider {
	protected ProviderDescriptor descriptor;
	protected Path workingPath;

	public ProviderLibraries libraries() {
		return ProviderLibraries.empty();
	}

	public List<Module> modules() {
		return List.of();
	}

	/**
	 * Define the authentication step pipeline for this provider.
	 *
	 * @return ordered list of step instances
	 */
	public abstract List<AuthenticationStep> getAuthenticationSteps();

	/**
	 * Declare which fields this provider uses for conflict detection.
	 *
	 * @return set of conflict key names
	 */
	public abstract Set<String> getConflictKeys();

	/**
	 * Declare which conflict solution IDs this provider supports.
	 *
	 * @return set of supported solution IDs
	 */
	public abstract Set<String> getAvailableConflictSolutions();

	/**
	 * Apply the specified conflict solution to resolve a conflict.
	 *
	 * @param solutionId the solution ID from {@link #getAvailableConflictSolutions()}
	 * @param claim the new authentication claim attempting to join
	 * @param context conflict context (existing sessions, conflict key/value, etc.)
	 * @return resolution result (allow/deny, username modification, etc.)
	 */
	public abstract ConflictResolution applyConflictSolution(
			String solutionId,
			IdentityClaim claim,
			ConflictContext context
	);

	public void onLoad() {

	}

	public void onEnable() {

	}

	public void onDisable() {

	}

	public void onUnload() {

	}
}
