package me.whereareiam.identica.pipeline.journey.rule;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface JourneyRuleRegistry {
	void register(@NotNull JourneyRule rule);

	boolean unregister(@NotNull String ruleId);

	@NotNull List<JourneyRule> resolve();
}
