package me.whereareiam.identica.provider.credential.pipeline.scenario.registration;

import com.google.inject.Provider;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.credential.pipeline.CredentialAuthenticationAttempt;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegisterStateItem;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegistrationAttempt;
import me.whereareiam.identica.provider.credential.pipeline.scenario.AbstractCredentialStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract class AbstractCredentialRegistrationStep extends AbstractCredentialStep {
	protected final Provider<CredentialMessages> messagesProvider;
	protected final Provider<Settings> coreSettingsProvider;
	protected final PipelineStateStore pipelineStateStore;

	protected AbstractCredentialRegistrationStep(
			@NotNull String name,
			@NotNull Provider<CredentialMessages> messagesProvider,
			@NotNull Provider<Settings> coreSettingsProvider,
			@NotNull PipelineStateStore pipelineStateStore
	) {
		super(name);
		this.messagesProvider = messagesProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.pipelineStateStore = pipelineStateStore;
	}

	protected long registrationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null) return 0L;
		return settings.getConnection().getRegistration().pipelineTtlMillis();
	}

	protected @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	protected @Nullable CredentialRegistrationAttempt consumeRegistrationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CredentialRegistrationAttempt input = stored.item(CredentialRegistrationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CredentialRegistrationAttempt.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
		return input;
	}

	protected @Nullable CredentialAuthenticationAttempt consumeAuthenticationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CredentialAuthenticationAttempt input = stored.item(CredentialAuthenticationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CredentialAuthenticationAttempt.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
		return input;
	}

	protected @Nullable CredentialRegisterStateItem getRegisterState(@NotNull ScenarioContext context) {
		PipelineState stored = pipelineStateStore.find(reference(context)).orElse(null);
		return stored != null ? stored.item(CredentialRegisterStateItem.class).orElse(null) : null;
	}

	protected void storeRegisterState(
			@NotNull ScenarioContext context,
			@NotNull CredentialRegisterStateItem stateItem,
			long ttlMs
	) {
		if (ttlMs <= 0) return;
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return;

		stored.putItem(stateItem, ttlMs);
		pipelineStateStore.save(reference, stored, ttlMs);
	}

	protected void clearRegisterState(@NotNull ScenarioContext context, long ttlMs) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return;

		stored.removeItem(CredentialRegisterStateItem.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
	}

	protected static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
