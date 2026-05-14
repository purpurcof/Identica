package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionService;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class PremiumRecognitionStep extends InteractiveStep {
	private final SessionRecognitionService sessionRecognitionService;

	@Inject
	public PremiumRecognitionStep(SessionRecognitionService sessionRecognitionService) {
		super("premium-recognition");
		this.sessionRecognitionService = sessionRecognitionService;
	}

	@Override
	public int order() {
		return 35;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		ProviderContext provider = context.getProvider();
		if (provider == null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		boolean recognized = sessionRecognitionService.matches(
				provider.getProviderId(),
				provider.getProviderSubject(),
				provider.getProviderUsername(),
				context.getIp(),
				context.getIdentity().getOrigin()
		);
		return CompletableFuture.completedFuture(recognized
				? StepResult.complete(context)
				: StepResult.proceed(context));
	}
}
