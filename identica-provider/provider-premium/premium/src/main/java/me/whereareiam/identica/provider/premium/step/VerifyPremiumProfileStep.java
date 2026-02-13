package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.journey.step.type.SeamlessStep;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.PremiumProfileIdItem;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.handshake.PremiumForceOnlineInstruction;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakeAttributes;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class VerifyPremiumProfileStep extends SeamlessStep {
	private final Provider<PremiumMessages> messagesProvider;
	private final PipelineStateStore pipelineStateStore;
	private final HandshakeStore handshakeStore;
	private final Provider<Settings> settingsProvider;

	@Inject
	public VerifyPremiumProfileStep(
			Provider<PremiumMessages> messagesProvider,
			PipelineStateStore pipelineStateStore,
			HandshakeStore handshakeStore,
			Provider<Settings> settingsProvider
	) {
		super("verify");
		this.messagesProvider = messagesProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.handshakeStore = handshakeStore;
		this.settingsProvider = settingsProvider;
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		PremiumMessages.Verification verification = messagesProvider.get().getVerification();
		String username = context.getUsername();
		String ip = context.getIp();
		if (username == null || username.isBlank() || ip == null || ip.isBlank())
			return CompletableFuture.completedFuture(failed(verification));

		PipelineStateReference reference = PipelineStateReference.builder()
				.username(username)
				.ip(ip)
				.build();
		String providerSubject = pipelineStateStore.find(reference)
				.flatMap(state -> state.item(PremiumProfileIdItem.class))
				.map(PremiumProfileIdItem::getProfileId)
				.orElse(null);
		if (providerSubject != null && !providerSubject.isBlank())
			pipelineStateStore.clear(reference);
		if (providerSubject == null || providerSubject.isBlank())
			return CompletableFuture.completedFuture(failed(verification));

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && providerSubject.equalsIgnoreCase(offlineUuid.toString()))
			return CompletableFuture.completedFuture(requireReconnect(verification, username, ip));

		return CompletableFuture.completedFuture(completeWithProfile(context, providerSubject, username));
	}

	private StepResult completeWithProfile(ScenarioContext context, String providerSubject, String username) {
		ProviderContext provider = ProviderContext.builder()
				.providerId(PremiumConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.providerUsername(username)
				.build();

		context.setProvider(provider);

		return StepResult.complete(context);
	}

	private StepResult requireReconnect(PremiumMessages.Verification verification, String username, String ip) {
		String message = joinLines(preferRejoinMessage(verification));
		requestForceOnline(username, ip);
		return StepResult.requireReconnect(message);
	}

	private StepResult failed(PremiumMessages.Verification verification) {
		return StepResult.failed(joinLines(verification.getInvalidSession()));
	}

	private List<String> preferRejoinMessage(PremiumMessages.Verification verification) {
		List<String> rejoin = verification.getRejoin();
		return rejoin != null && !rejoin.isEmpty()
				? rejoin
				: verification.getInvalidSession();
	}

	private void requestForceOnline(String username, String ip) {
		if (username == null || username.isBlank())
			return;

		long ttlMillis = settingsProvider.get()
				.getConnection()
				.handshakeInstructionTtlMillis();
		HandshakeInstruction instruction = HandshakeInstruction.create(
				new ConnectionIdentity(username, ip),
				ttlMillis
		);
		instruction.setAttribute(PremiumHandshakeAttributes.FORCE_ONLINE,
				new PremiumForceOnlineInstruction("verify"));
		handshakeStore.putInstruction(instruction);
	}

	private String joinLines(List<String> lines) {
		return String.join("\n", lines);
	}
}
