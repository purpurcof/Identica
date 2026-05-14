package me.whereareiam.identica.provider.credential.completion;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtension;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionBuilder;
import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CredentialCompletionExtension implements CompletionExtension {
	private final CredentialCompletionStep passwordCompletionStep;

	public static @NotNull String extensionId() {
		return CredentialConstants.PROVIDER_ID + ":completion";
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
		builder.registerStep(CredentialConstants.PROVIDER_ID, PipelineType.AUTHENTICATION, passwordCompletionStep);
		builder.registerStep(CredentialConstants.PROVIDER_ID, PipelineType.REGISTRATION, passwordCompletionStep);
		builder.registerStep(CredentialConstants.PROVIDER_ID, PipelineType.MIGRATION, passwordCompletionStep);
	}
}
