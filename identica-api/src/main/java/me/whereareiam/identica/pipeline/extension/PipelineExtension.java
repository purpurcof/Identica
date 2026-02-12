package me.whereareiam.identica.pipeline.extension;

import org.jetbrains.annotations.NotNull;

public interface PipelineExtension {
	@NotNull String id();

	default int order() {
		return 0;
	}

	void apply(@NotNull PipelineExtensionBuilder builder);
}
