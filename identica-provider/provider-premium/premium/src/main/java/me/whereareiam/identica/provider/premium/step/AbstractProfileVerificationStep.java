package me.whereareiam.identica.provider.premium.step;

import com.google.inject.Provider;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.SeamlessStep;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.provider.premium.PremiumVerifyAttemptItem;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileSnapshot;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractProfileVerificationStep extends SeamlessStep {
	protected final Provider<PremiumMessages> messagesProvider;
	protected final PipelineStateStore pipelineStateStore;
	protected final PremiumProfileStore profileStore;
	protected final HandshakeStore handshakeStore;
	protected final Provider<Settings> settingsProvider;

	protected AbstractProfileVerificationStep(
			String name,
			Provider<PremiumMessages> messagesProvider,
			PipelineStateStore pipelineStateStore,
			PremiumProfileStore profileStore,
			HandshakeStore handshakeStore,
			Provider<Settings> settingsProvider
	) {
		super(name);
		this.messagesProvider = messagesProvider;
		this.pipelineStateStore = pipelineStateStore;
		this.profileStore = profileStore;
		this.handshakeStore = handshakeStore;
		this.settingsProvider = settingsProvider;
	}

	protected @NotNull PremiumMessages.Verification verification() {
		return messagesProvider.get().getVerification();
	}

	protected @Nullable String readProfileId(@NotNull String username) {
		PremiumProfileSnapshot snapshot = profileStore.find(username);
		return snapshot != null ? snapshot.getProfileId() : null;
	}

	protected boolean hasAttempt(@NotNull ScenarioContext context) {
		return pipelineStateStore.find(PipelineStateReference.from(context))
				.flatMap(state -> state.item(PremiumVerifyAttemptItem.class))
				.isPresent();
	}

	protected void clearAttempt(@NotNull ScenarioContext context) {
		long ttlMillis = ttlMillis();
		pipelineStateStore.update(PipelineStateReference.from(context), ttlMillis,
				state -> state.withoutItem(PremiumVerifyAttemptItem.class));
	}

	protected void clearProfileItem(@NotNull String username) {
		profileStore.clear(username);
	}

	protected StepResult failed() {
		return StepResult.failed("");
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
