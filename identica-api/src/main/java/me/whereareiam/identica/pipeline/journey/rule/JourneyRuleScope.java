package me.whereareiam.identica.pipeline.journey.rule;

import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;

public interface JourneyRuleScope {
	boolean matches(@NotNull JourneyStage stage);

	static @NotNull JourneyRuleScope forStageType(@NotNull StageType type) {
		return stage -> type.equals(stage.getType());
	}
}
