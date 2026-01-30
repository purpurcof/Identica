package me.whereareiam.identica.auth.step;

import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;

import java.util.concurrent.CompletableFuture;

/**
 * Base interface for authentication steps.
 * Steps are instantiated by providers and executed by core.
 * Implementations should extend {@link me.whereareiam.identica.auth.step.type.SeamlessStep}
 * or {@link me.whereareiam.identica.auth.step.type.InteractiveStep} so core can determine
 * interaction requirements.
 */
public interface AuthenticationStep {
	/**
	 * Step name used for routing configuration.
	 * Must be unique within a eligibility.
	 */
	String getName();

	/**
	 * Check if this step should execute based on context state.
	 * Default implementation always returns true.
	 */
	default boolean shouldExecute(AuthContext context) {
		return true;
	}

	/**
	 * Execute this step.
	 *
	 * @param context current authentication context with state
	 * @return step result indicating what to do next
	 */
	CompletableFuture<StepResult> execute(AuthContext context);
}
