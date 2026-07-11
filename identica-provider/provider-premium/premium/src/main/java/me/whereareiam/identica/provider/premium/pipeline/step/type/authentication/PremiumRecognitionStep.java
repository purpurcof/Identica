package me.whereareiam.identica.provider.premium.pipeline.step.type.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.provider.capability.recognition.SessionRecognitionService;
import me.whereareiam.identica.provider.capability.recognition.store.RecognizedConnectionStore;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class PremiumRecognitionStep extends InteractiveStep {
	private final SessionRecognitionService sessionRecognitionService;
	private final RecognizedConnectionStore recognizedConnectionStore;

	@Inject
	public PremiumRecognitionStep(
			SessionRecognitionService sessionRecognitionService,
			RecognizedConnectionStore recognizedConnectionStore
	) {
		super("premium-recognition");
		this.sessionRecognitionService = sessionRecognitionService;
		this.recognizedConnectionStore = recognizedConnectionStore;
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
		if (recognized && context.getIdentity().getConnectionUniqueId() != null)
			recognizedConnectionStore.markRecognized(context.getIdentity().getConnectionUniqueId());

		return CompletableFuture.completedFuture(recognized
				? StepResult.complete(context)
				: StepResult.proceed(context));
	}
}
