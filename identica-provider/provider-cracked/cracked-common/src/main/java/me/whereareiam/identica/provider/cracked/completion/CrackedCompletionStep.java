package me.whereareiam.identica.provider.cracked.completion;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.pipeline.completion.step.AbstractMessageCompletionStep;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
public class CrackedCompletionStep extends AbstractMessageCompletionStep {
	private final Provider<CrackedMessages> messagesProvider;

	@Inject
	public CrackedCompletionStep(Provider<CrackedMessages> messagesProvider) {
		super("cracked-completion");
		this.messagesProvider = messagesProvider;
	}

	@Override
	protected @Nullable AbstractMessageCompletionStep.TitleContent title(@NotNull CompletionContext context) {
		CrackedMessages.Completion.Pipeline messages = resolve(context);
		if (messages == null || messages.getTitle() == null) return null;

		return title(
				messages.getTitle().getTitle(),
				messages.getTitle().getSubtitle()
		);
	}

	@Override
	protected @Nullable List<String> messageLines(@NotNull CompletionContext context) {
		CrackedMessages.Completion.Pipeline completion = resolve(context);
		if (completion != null && completion.getBody() != null && !completion.getBody().isEmpty())
			return completion.getBody();

		return null;
	}

	private @Nullable CrackedMessages.Completion.Pipeline resolve(@NotNull CompletionContext context) {
		CrackedMessages.Completion completion = messagesProvider.get().getCompletion();
		if (completion == null) return null;

		return switch (context.getPipelineType()) {
			case AUTHENTICATION -> completion.getAuthentication();
			case REGISTRATION -> completion.getRegistration();
			case MIGRATION -> completion.getMigration();
		};
	}
}
