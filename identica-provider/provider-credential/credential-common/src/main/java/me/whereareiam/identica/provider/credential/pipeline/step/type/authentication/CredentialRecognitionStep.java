package me.whereareiam.identica.provider.credential.pipeline.step.type.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.provider.capability.recognition.SessionRecognitionService;
import me.whereareiam.identica.provider.capability.recognition.store.RecognizedConnectionStore;
import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.provider.credential.pipeline.step.base.AbstractCredentialStep;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class CredentialRecognitionStep extends AbstractCredentialStep {
	private final SessionRecognitionService sessionRecognitionService;
	private final RecognizedConnectionStore recognizedConnectionStore;

	@Inject
	public CredentialRecognitionStep(
			SessionRecognitionService sessionRecognitionService,
			RecognizedConnectionStore recognizedConnectionStore
	) {
		super("password-recognition");
		this.sessionRecognitionService = sessionRecognitionService;
		this.recognizedConnectionStore = recognizedConnectionStore;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String providerSubject = requireProviderSubject(context);
		boolean recognized = sessionRecognitionService.matches(
				CredentialConstants.PROVIDER_ID,
				providerSubject,
				context.getUsername(),
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
