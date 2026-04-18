package me.whereareiam.identica.provider.premium.completion;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.pipeline.completion.step.AbstractMessageCompletionStep;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
public class PremiumCompletionStep extends AbstractMessageCompletionStep {
	private final Provider<PremiumMessages> messagesProvider;

	@Inject
	public PremiumCompletionStep(Provider<PremiumMessages> messagesProvider) {
		super("premium-completion");
		this.messagesProvider = messagesProvider;
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

		if (context.getPipelineType() == PipelineType.MIGRATION)
			return completion.getMigration();

		if (context.isSessionReused() && completion.getSession() != null)
			return completion.getSession();

		return completion.getAuthentication();
	}
}
