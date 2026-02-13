package me.whereareiam.identica.pipeline.extension;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PipelineExtensionRegistry {
	void register(@NotNull PipelineExtension extension);

	boolean unregister(@NotNull String extensionId);

	@NotNull List<PipelineExtension> getAll();
}
