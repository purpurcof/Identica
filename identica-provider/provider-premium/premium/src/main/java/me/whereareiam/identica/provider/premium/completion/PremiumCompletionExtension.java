package me.whereareiam.identica.provider.premium.completion;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtension;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionBuilder;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class PremiumCompletionExtension implements CompletionExtension {
	private final @NotNull String providerId;
	private final @NotNull PremiumCompletionStep premiumCompletionStep;

	public static @NotNull String extensionIdFor(@NotNull String providerId) {
		return providerId + ":completion";
	}

	@Override
	public @NotNull String id() {
		return extensionIdFor(providerId);
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public void apply(@NotNull CompletionExtensionBuilder builder) {
		builder.registerStep(providerId, PipelineType.AUTHENTICATION, premiumCompletionStep);
		builder.registerStep(providerId, PipelineType.MIGRATION, premiumCompletionStep);
	}
}
