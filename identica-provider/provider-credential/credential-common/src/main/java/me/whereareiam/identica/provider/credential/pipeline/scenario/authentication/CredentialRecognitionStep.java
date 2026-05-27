package me.whereareiam.identica.provider.credential.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionService;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.AuthenticationRecognitionStep;
import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.provider.credential.pipeline.scenario.base.AbstractCredentialStep;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class CredentialRecognitionStep extends AbstractCredentialStep implements AuthenticationRecognitionStep {
	private final SessionRecognitionService sessionRecognitionService;

	@Inject
	public CredentialRecognitionStep(SessionRecognitionService sessionRecognitionService) {
		super("password-recognition");
		this.sessionRecognitionService = sessionRecognitionService;
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
		return CompletableFuture.completedFuture(recognized
				? StepResult.complete(context)
				: StepResult.proceed(context));
	}
}
