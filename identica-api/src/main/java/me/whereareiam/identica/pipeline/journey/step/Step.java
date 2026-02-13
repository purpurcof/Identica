package me.whereareiam.identica.pipeline.journey.step;

import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface Step {
	@NotNull String getName();

	default int order() {
		return 0;
	}

	@SuppressWarnings("unused")
	default boolean shouldExecute(@NotNull ScenarioContext context) {
		return true;
	}

	@NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context);
}
