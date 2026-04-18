package me.whereareiam.identica.pipeline.completion.extension;

import me.whereareiam.identica.pipeline.completion.step.CompletionStep;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CompletionExtensionBuilder {
	void registerStep(
			@Nullable String providerId,
			@NotNull PipelineType pipelineType,
			@NotNull CompletionStep step
	);

	default void registerStep(
			@Nullable String providerId,
			@NotNull CompletionStep step
	) {
		registerStep(providerId, PipelineType.AUTHENTICATION, step);
		registerStep(providerId, PipelineType.REGISTRATION, step);
		registerStep(providerId, PipelineType.MIGRATION, step);
	}
}
