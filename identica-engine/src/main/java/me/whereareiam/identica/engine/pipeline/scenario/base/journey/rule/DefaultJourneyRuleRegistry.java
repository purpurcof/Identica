package me.whereareiam.identica.engine.pipeline.scenario.base.journey.rule;

import com.google.inject.Singleton;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRule;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRuleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class DefaultJourneyRuleRegistry implements JourneyRuleRegistry {
	private final List<JourneyRule> rules = new CopyOnWriteArrayList<>();

	@Override
	public void register(@NotNull JourneyRule rule) {
		String id = rule.id();
		if (id.isBlank())
			return;
		String normalized = normalize(id);
		rules.removeIf(existing -> existing != null && normalize(existing.id()).equals(normalized));
		rules.add(rule);
	}

	@Override
	public boolean unregister(@NotNull String ruleId) {
		if (ruleId.isBlank())
			return false;
		String normalized = normalize(ruleId);
		return rules.removeIf(existing -> existing != null && normalize(existing.id()).equals(normalized));
	}

	@Override
	public @NotNull List<JourneyRule> resolve() {
		return List.copyOf(rules);
	}

	private @NotNull String normalize(@NotNull String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
