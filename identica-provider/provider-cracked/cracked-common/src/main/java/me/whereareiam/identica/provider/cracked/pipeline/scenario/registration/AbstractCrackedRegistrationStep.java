package me.whereareiam.identica.provider.cracked.pipeline.scenario.registration;

import com.google.inject.Provider;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.AbstractCrackedStep;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedAuthenticationAttempt;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegistrationAttempt;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegisterStateItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract class AbstractCrackedRegistrationStep extends AbstractCrackedStep {
	protected final Provider<CrackedMessages> messagesProvider;
	protected final Provider<Settings> coreSettingsProvider;
	protected final PipelineStateStore pipelineStateStore;

	protected AbstractCrackedRegistrationStep(
			@NotNull String name,
			@NotNull Provider<CrackedMessages> messagesProvider,
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
		if (settings == null)
			return 0L;
		return settings.getConnection().getRegistration().pipelineTtlMillis();
	}

	protected @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	protected @Nullable CrackedRegistrationAttempt consumeRegistrationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CrackedRegistrationAttempt input = stored.item(CrackedRegistrationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CrackedRegistrationAttempt.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
		return input;
	}

	protected @Nullable CrackedAuthenticationAttempt consumeAuthenticationAttempt(
			@NotNull ScenarioContext context,
			long ttlMs
	) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return null;

		CrackedAuthenticationAttempt input = stored.item(CrackedAuthenticationAttempt.class).orElse(null);
		if (input == null) return null;

		stored.removeItem(CrackedAuthenticationAttempt.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
		return input;
	}

	protected @Nullable CrackedRegisterStateItem getRegisterState(@NotNull ScenarioContext context) {
		PipelineState stored = pipelineStateStore.find(reference(context)).orElse(null);
		return stored != null ? stored.item(CrackedRegisterStateItem.class).orElse(null) : null;
	}

	protected void storeRegisterState(
			@NotNull ScenarioContext context,
			@NotNull CrackedRegisterStateItem stateItem,
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

		stored.removeItem(CrackedRegisterStateItem.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
	}

	protected static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty())
			return "";
		return String.join("\n", lines);
	}
}
