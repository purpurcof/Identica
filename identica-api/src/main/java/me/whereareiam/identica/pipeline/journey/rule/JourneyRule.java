package me.whereareiam.identica.pipeline.journey.rule;

import me.whereareiam.identica.model.pipeline.journey.JourneyRuleContext;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionPlan;
import org.jetbrains.annotations.NotNull;

/**
 * A scoped rule that can filter/reorder/replace execution stages within a journey plan.
 * Implementations must only produce stages that match their {@link #scope()}.
 */
public interface JourneyRule {
	/**
	 * Stable identifier used for registration and overrides.
	 */
	@NotNull String id();

	/**
	 * Execution order, lower runs first.
	 */
	int order();

	/**
	 * Limits which stages the rule can read/write.
	 */
	@NotNull JourneyRuleScope scope();

	/**
	 * Optional guard for conditional rules.
	 */
	@SuppressWarnings("unused")
	default boolean supports(@NotNull JourneyRuleContext ctx) {
		return true;
	}

	/**
	 * Applies rule changes to the scoped portion of the current plan.
	 * Implementations must not emit stages outside of {@link #scope()}.
	 */
	@NotNull JourneyExecutionPlan apply(
			@NotNull JourneyRuleContext ctx,
			@NotNull JourneyExecutionPlan current
	);
}
