package me.whereareiam.identica.provider.credential.pipeline.scenario.base;

import com.google.inject.Provider;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.credential.pipeline.CredentialAuthenticationAttempt;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegisterStateItem;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegistrationAttempt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractCredentialStatefulStep extends AbstractCredentialStep {
	protected final Provider<Settings> coreSettingsProvider;
	protected final PipelineStateStore pipelineStateStore;

	protected AbstractCredentialStatefulStep(
			@NotNull String name,
			@NotNull Provider<Settings> coreSettingsProvider,
			@NotNull PipelineStateStore pipelineStateStore
	) {
		super(name);
		this.coreSettingsProvider = coreSettingsProvider;
		this.pipelineStateStore = pipelineStateStore;
	}

	protected final long authenticationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null) return 0L;
		return settings.getConnection().getScenarios().getAuthentication().pipelineTtlMillis();
	}

	protected final long registrationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null) return 0L;
		return settings.getConnection().getScenarios().getRegistration().pipelineTtlMillis();
	}

	protected final long migrationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null) return 0L;
		return settings.getConnection().getScenarios().getMigration().pipelineTtlMillis();
	}

	protected final @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	protected final @Nullable CredentialRegistrationAttempt consumeRegistrationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CredentialRegistrationAttempt input = stored.item(CredentialRegistrationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CredentialRegistrationAttempt.class);
		if (ttlMs > 0)
			pipelineStateStore.save(reference, stored, ttlMs);
		return input;
	}

	protected final @Nullable CredentialAuthenticationAttempt consumeAuthenticationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CredentialAuthenticationAttempt input = stored.item(CredentialAuthenticationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CredentialAuthenticationAttempt.class);
		if (ttlMs > 0)
			pipelineStateStore.save(reference, stored, ttlMs);
		return input;
	}

	protected final @Nullable CredentialRegisterStateItem getRegisterState(@NotNull ScenarioContext context) {
		PipelineState stored = pipelineStateStore.find(reference(context)).orElse(null);
		return stored != null ? stored.item(CredentialRegisterStateItem.class).orElse(null) : null;
	}

	protected final void storeRegisterState(
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

	protected final void clearRegisterState(@NotNull ScenarioContext context, long ttlMs) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return;

		stored.removeItem(CredentialRegisterStateItem.class);
		if (ttlMs > 0)
			pipelineStateStore.save(reference, stored, ttlMs);
	}

	protected static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
