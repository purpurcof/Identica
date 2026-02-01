package me.whereareiam.identica.auth.step;

import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.type.step.StepAudience;
import org.jetbrains.annotations.NotNull;

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
	@NotNull String getName();

	/**
	 * Declares which audience this step targets.
	 *
	 * @return step audience
	 */
	default @NotNull StepAudience getAudience() {
		return StepAudience.ALL;
	}

	/**
	 * Check if this step should execute based on context state.
	 * Default implementation always returns true.
	 */
	default boolean shouldExecute(@NotNull AuthContext context) {
		return true;
	}

	/**
	 * Execute this step.
	 *
	 * @param context current authentication context with state
	 * @return step result indicating what to do next
	 */
	@NotNull CompletableFuture<StepResult> execute(@NotNull AuthContext context);
}
