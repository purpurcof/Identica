package me.whereareiam.identica.pipeline.completion.extension;

import org.jetbrains.annotations.NotNull;

public interface CompletionExtension {
	@NotNull String id();

	default int order() {
		return 0;
	}

	void apply(@NotNull CompletionExtensionBuilder builder);
}
