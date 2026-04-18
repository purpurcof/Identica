package me.whereareiam.identica.pipeline.completion.extension;

import me.whereareiam.identica.pipeline.completion.step.CompletionStep;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public interface CompletionExtensionRegistry {
	void register(@NotNull CompletionExtension extension);

	boolean unregister(@NotNull String extensionId);

	@NotNull List<CompletionExtension> getAll();

	@NotNull List<CompletionStep> resolve(@Nullable String providerId, @NotNull PipelineType pipelineType);
}
