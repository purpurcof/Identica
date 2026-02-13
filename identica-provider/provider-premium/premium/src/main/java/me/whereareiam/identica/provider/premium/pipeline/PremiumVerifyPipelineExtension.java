package me.whereareiam.identica.provider.premium.pipeline;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.extension.PipelineExtension;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionBuilder;
import me.whereareiam.identica.provider.premium.step.VerifyPremiumProfileStep;
import me.whereareiam.identica.type.pipeline.PipelineScope;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class PremiumVerifyPipelineExtension implements PipelineExtension {
	private final @NotNull String providerId;

	// Steps
	private final @NotNull VerifyPremiumProfileStep verifyPremiumProfileStep;

	public static @NotNull String extensionIdFor(@NotNull String providerId) {
		return providerId + ":verify-resolver";
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
	public void apply(@NotNull PipelineExtensionBuilder builder) {
		builder.registerStep(
				providerId,
				StageType.PROVIDER,
				verifyPremiumProfileStep
		);

		builder.registerStep(
				PipelineScope.MIGRATION,
				providerId,
				StageType.PROVIDER,
				PipelineType.MIGRATION,
				verifyPremiumProfileStep
		);
	}
}
