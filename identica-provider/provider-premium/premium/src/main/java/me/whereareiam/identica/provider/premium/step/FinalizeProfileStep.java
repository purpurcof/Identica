package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class FinalizeProfileStep extends AbstractProfileVerificationStep {
	@Inject
	public FinalizeProfileStep(
			Provider<PremiumMessages> messagesProvider,
			PipelineStateStore pipelineStateStore,
			HandshakeStore handshakeStore,
			Provider<Settings> settingsProvider
	) {
		super("finalize-profile", messagesProvider, pipelineStateStore, handshakeStore, settingsProvider);
	}

	@Override
	public int order() {
		return 30;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		PremiumMessages.Verification verification = verification();
		String username = context.getUsername();
		String ip = context.getIp();
		if (username == null || username.isBlank() || ip == null || ip.isBlank())
			return CompletableFuture.completedFuture(failed(verification));

		PipelineStateReference reference = referenceFor(username, ip);
		String providerSubject = readProfileId(reference);
		if (providerSubject == null || providerSubject.isBlank()) {
			return CompletableFuture.completedFuture(failed(verification));
		}

		if (hasAttempt(reference)) clearAttempt(reference);
		ProviderContext provider = ProviderContext.builder()
				.providerId(PremiumConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.providerUsername(username)
				.build();

		context.setProvider(provider);
		clearProfileItem(reference);

		return CompletableFuture.completedFuture(StepResult.complete(context));
	}
}
