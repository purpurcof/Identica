package me.whereareiam.identica.provider.premium.completion;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.pipeline.completion.step.AbstractMessageCompletionStep;
import me.whereareiam.identica.provider.capability.recognition.store.RecognizedConnectionStore;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Singleton
public class PremiumCompletionStep extends AbstractMessageCompletionStep {
	private final Provider<PremiumMessages> messagesProvider;
	private final RecognizedConnectionStore recognizedConnectionStore;

	@Inject
	public PremiumCompletionStep(
			Provider<PremiumMessages> messagesProvider,
			RecognizedConnectionStore recognizedConnectionStore
	) {
		super("premium-completion");
		this.messagesProvider = messagesProvider;
		this.recognizedConnectionStore = recognizedConnectionStore;
	}

	@Override
	protected @Nullable AbstractMessageCompletionStep.TitleContent title(@NotNull CompletionContext context) {
		PremiumMessages.Completion.Pipeline messages = resolve(context);
		if (messages.getTitle() == null) return null;

		return title(
				messages.getTitle().getTitle(),
				messages.getTitle().getSubtitle()
		);
	}

	@Override
	protected @Nullable List<String> messageLines(@NotNull CompletionContext context) {
		PremiumMessages.Completion.Pipeline messages = resolve(context);
		return messages.getBody();
	}

	private PremiumMessages.Completion.@NotNull Pipeline resolve(@NotNull CompletionContext context) {
		PremiumMessages.Completion completion = messagesProvider.get().getCompletion();

		if (context.getPipelineType() == PipelineType.MIGRATION) return completion.getMigration();
		if (context.getPipelineType() == PipelineType.REGISTRATION) return completion.getRegistration();
		if (isRecognized(context)) return completion.getSession();

		return completion.getAuthentication();
	}

	private boolean isRecognized(@NotNull CompletionContext context) {
		UUID connectionUniqueId = context.getIdentity().getConnectionUniqueId();
		return connectionUniqueId != null && recognizedConnectionStore.isRecognized(connectionUniqueId);
	}
}
