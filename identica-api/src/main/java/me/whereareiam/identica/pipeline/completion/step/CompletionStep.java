package me.whereareiam.identica.pipeline.completion.step;

import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import org.jetbrains.annotations.NotNull;

public interface CompletionStep {
	@NotNull String getName();

	default int order() {
		return 0;
	}

	default boolean shouldExecute(@NotNull CompletionContext context) {
		return true;
	}

	void execute(@NotNull CompletionContext context);
}
