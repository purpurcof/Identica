package me.whereareiam.identica.provider.cracked.completion;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtension;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionBuilder;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrackedCompletionExtension implements CompletionExtension {
	private final CrackedCompletionStep crackedCompletionStep;

	public static @NotNull String extensionId() {
		return CrackedConstants.PROVIDER_ID + ":completion";
	}

	@Override
	public @NotNull String id() {
		return extensionId();
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public void apply(@NotNull CompletionExtensionBuilder builder) {
		builder.registerStep(CrackedConstants.PROVIDER_ID, PipelineType.AUTHENTICATION, crackedCompletionStep);
		builder.registerStep(CrackedConstants.PROVIDER_ID, PipelineType.REGISTRATION, crackedCompletionStep);
		builder.registerStep(CrackedConstants.PROVIDER_ID, PipelineType.MIGRATION, crackedCompletionStep);
	}
}
