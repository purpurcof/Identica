package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.model.config.Engine;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class FinalizeProfileStep extends AbstractProfileVerificationStep {
	@Inject
	public FinalizeProfileStep(
			Provider<PremiumMessages> messagesProvider,
			PipelineStateStore pipelineStateStore,
			PremiumProfileStore profileStore,
			HandshakeStore handshakeStore,
			Provider<Engine> engineProvider
	) {
		super("finalize-profile", messagesProvider, pipelineStateStore, profileStore, handshakeStore, engineProvider);
	}

	@Override
	public int order() {
		return 30;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
        String username = context.getUsername();
		String ip = context.getIp();
		if (username == null || username.isBlank() || ip == null || ip.isBlank())
			return CompletableFuture.completedFuture(failed());

		String providerSubject = readProfileId(username);
		if (providerSubject == null || providerSubject.isBlank()) {
			return CompletableFuture.completedFuture(failed());
		}

		if (hasAttempt(context)) clearAttempt(context);
		ProviderContext existing = context.getProvider();
		ProviderContext provider = ProviderContext.builder()
				.providerId(PremiumConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.providerUsername(username)
				.source(existing != null ? existing.getSource() : null)
				.build();

		context.setProvider(provider);
		clearProfileItem(username);
		Logger.debug(
				"Premium finalize applied provider context connection=%s username=%s ip=%s subject=%s",
				context.getConnectionUniqueId(),
				username,
				ip,
				providerSubject
		);

		StepResult result = context instanceof RegistrationContext
				? StepResult.complete(context)
				: StepResult.proceed(context);
		return CompletableFuture.completedFuture(result);
	}
}
