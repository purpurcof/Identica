package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Provider;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.journey.step.type.SeamlessStep;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.premium.PremiumIdentityMetaItem;
import me.whereareiam.identica.provider.premium.PremiumVerifyAttemptItem;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractProfileVerificationStep extends SeamlessStep {
	protected final Provider<PremiumMessages> messagesProvider;
	protected final PipelineStateStore pipelineStateStore;
	protected final HandshakeStore handshakeStore;
	protected final Provider<Settings> settingsProvider;

	protected AbstractProfileVerificationStep(
			String name,
			Provider<PremiumMessages> messagesProvider,
			PipelineStateStore pipelineStateStore,
			HandshakeStore handshakeStore,
			Provider<Settings> settingsProvider
	) {
		super(name);
		this.messagesProvider = messagesProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.handshakeStore = handshakeStore;
		this.settingsProvider = settingsProvider;
	}

	protected @NotNull PremiumMessages.Verification verification() {
		return messagesProvider.get().getVerification();
	}

	protected @NotNull PipelineStateReference referenceFor(@NotNull String username, @NotNull String ip) {
		return PipelineStateReference.builder()
				.username(username)
				.ip(ip)
				.build();
	}

	protected @Nullable String readProfileId(@NotNull PipelineStateReference reference) {
		return pipelineStateStore.find(reference)
				.flatMap(state -> state.item(PremiumIdentityMetaItem.class))
				.map(PremiumIdentityMetaItem::getProfileId)
				.orElse(null);
	}

	protected boolean hasAttempt(@NotNull PipelineStateReference reference) {
		return pipelineStateStore.find(reference)
				.flatMap(state -> state.item(PremiumVerifyAttemptItem.class))
				.isPresent();
	}

	protected void markAttempt(@NotNull PipelineStateReference reference) {
		long ttlMillis = ttlMillis();
		pipelineStateStore.update(reference, ttlMillis,
				state -> state.withItem(new PremiumVerifyAttemptItem(System.currentTimeMillis()), ttlMillis));
	}

	protected void clearAttempt(@NotNull PipelineStateReference reference) {
		long ttlMillis = ttlMillis();
		pipelineStateStore.update(reference, ttlMillis,
				state -> state.withoutItem(PremiumVerifyAttemptItem.class));
	}

	protected void clearProfileItem(@NotNull PipelineStateReference reference) {
		long ttlMillis = ttlMillis();
		pipelineStateStore.update(reference, ttlMillis,
				state -> state.withoutItem(PremiumIdentityMetaItem.class));
	}

	protected StepResult failed(PremiumMessages.Verification verification) {
		return StepResult.failed(joinLines(verification.getInvalidSession()));
	}

	protected long ttlMillis() {
		return settingsProvider.get()
				.getConnection()
				.handshakeInstructionTtlMillis();
	}

	protected @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
