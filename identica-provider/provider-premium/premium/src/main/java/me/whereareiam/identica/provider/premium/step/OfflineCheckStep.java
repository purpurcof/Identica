package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.handshake.PremiumForceOnlineInstruction;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakeAttributes;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class OfflineCheckStep extends AbstractProfileVerificationStep {
	@Inject
	public OfflineCheckStep(
			Provider<PremiumMessages> messagesProvider,
			PipelineStateStore pipelineStateStore,
			PremiumProfileStore profileStore,
			HandshakeStore handshakeStore,
			Provider<Settings> settingsProvider
	) {
		super("offline-check", messagesProvider, pipelineStateStore, profileStore, handshakeStore, settingsProvider);
	}

	@Override
	public int order() {
		return 20;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		PremiumMessages.Verification verification = verification();
		String username = context.getUsername();
		String ip = context.getIp();
		if (username == null || username.isBlank() || ip == null || ip.isBlank())
			return CompletableFuture.completedFuture(failed(verification));

		PipelineStateReference reference = referenceFor(username, ip);
		String providerSubject = readProfileId(username, ip);
		if (providerSubject == null || providerSubject.isBlank()) {
			return CompletableFuture.completedFuture(failed(verification));
		}

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && providerSubject.equalsIgnoreCase(offlineUuid.toString())) {
			if (!hasHandshakeAttempt(reference)) {
				markHandshakeAttempt(reference);
				return CompletableFuture.completedFuture(requireReconnect(verification, username, ip));
			}

			clearProfileItem(username, ip);
			handshakeStore.invalidateInstruction(username);
			return CompletableFuture.completedFuture(StepResult.failed(joinLines(verification.getInvalidSession())));
		}

		return CompletableFuture.completedFuture(StepResult.proceed(context));
	}

	private StepResult requireReconnect(
			PremiumMessages.Verification verification,
			String username,
			String ip
	) {
		requestForceOnline(username, ip);

		return StepResult.requireReconnect(joinLines(verification.getRejoin()));
	}

	private void requestForceOnline(String username, String ip) {
		if (username == null || username.isBlank()) return;

		long ttlMillis = ttlMillis();
		HandshakeInstruction instruction = HandshakeInstruction.create(
				new ConnectionIdentity(username, ip),
				ttlMillis
		);
		instruction.setAttribute(PremiumHandshakeAttributes.FORCE_ONLINE,
				new PremiumForceOnlineInstruction("verify"));
		handshakeStore.putInstruction(instruction);
	}

	private boolean hasHandshakeAttempt(@NotNull PipelineStateReference reference) {
		return profileStore.hasAttempt(reference.getUsername(), reference.getIp());
	}

	private void markHandshakeAttempt(@NotNull PipelineStateReference reference) {
		profileStore.markAttempt(reference.getUsername(), reference.getIp());
	}
}
