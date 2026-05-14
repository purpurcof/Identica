package me.whereareiam.identica.provider.credential.pipeline.scenario.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.credential.account.CredentialAccountService;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;
import me.whereareiam.identica.provider.credential.cryptography.CryptographyService;
import me.whereareiam.identica.provider.credential.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegisterStateItem;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegistrationAttempt;
import me.whereareiam.identica.provider.credential.pipeline.scenario.AbstractCredentialStep;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import me.whereareiam.identica.provider.credential.util.PasswordRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CredentialMigrationRegistrationStep extends AbstractCredentialStep {
	private final Provider<CredentialMessages> messagesProvider;
	private final Provider<CredentialSettings> settingsProvider;
	private final Provider<Settings> coreSettingsProvider;
	private final CredentialAccountService credentialService;
	private final CryptographyService cryptographyService;
	private final PasswordRules passwordPolicy;
	private final PipelineStateStore pipelineStateStore;

	@Inject
	public CredentialMigrationRegistrationStep(
			Provider<CredentialMessages> messagesProvider,
			Provider<CredentialSettings> settingsProvider,
			Provider<Settings> coreSettingsProvider,
			CredentialAccountService credentialService,
			CryptographyService cryptographyService,
			PasswordRules passwordPolicy,
			PipelineStateStore pipelineStateStore
	) {
		super("password-migration-registration");
		this.messagesProvider = messagesProvider;
		this.settingsProvider = settingsProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.credentialService = credentialService;
		this.cryptographyService = cryptographyService;
		this.passwordPolicy = passwordPolicy;
		this.pipelineStateStore = pipelineStateStore;
	}

	@Override
	public int order() {
		return 15;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String providerSubject = requireProviderSubject(context);

		CredentialAccount credential = credentialService.find(providerSubject).orElse(null);
		if (credential != null) return CompletableFuture.completedFuture(StepResult.proceed(context));

		CredentialMessages messages = messagesProvider.get();
		CredentialMessages.Scenario.Migration.Setup migrationMessages = messages.getScenario().getMigration().getSetup();
		CredentialSettings settings = settingsProvider.get();
		CredentialSettings.Scenario.Registration registrationSettings = settings != null
				&& settings.getScenario() != null
				? settings.getScenario().getRegistration()
				: null;

		if (registrationSettings == null || !registrationSettings.isEnabled())
			return CompletableFuture.completedFuture(StepResult.denied(migrationMessages.getStatus().getDisabled()));

		long ttlMs = migrationTtlMs();
		boolean requireRepeat = registrationSettings.isRequireRepeat();
		CredentialRegisterStateItem pending = getRegisterState(context);

		if (requireRepeat && pending != null) return CompletableFuture.completedFuture(StepResult.proceed(context));

		if (!requireRepeat && pending != null)
			clearRegisterState(context, ttlMs);

		CredentialRegistrationAttempt input = consumeRegistrationAttempt(context, ttlMs);
		if (input == null) return CompletableFuture.completedFuture(StepResult.waiting(joinLines(migrationMessages.getPrompt())));
		if (input.isConfirm()) return CompletableFuture.completedFuture(StepResult.waiting(migrationMessages.getStatus().getNoPending()));

		String error = passwordPolicy.validate(input.getPassword());
		if (error != null && !error.isBlank()) return CompletableFuture.completedFuture(StepResult.waiting(error));

		PasswordCandidate candidate = cryptographyService.hash(input.getPassword());
		if (candidate == null) return CompletableFuture.completedFuture(StepResult.failed(""));

		if (requireRepeat) {
			CredentialRegisterStateItem stateItem = new CredentialRegisterStateItem(
					candidate.getPasswordHash(),
					candidate.getHashingMethod()
			);
			storeRegisterState(context, stateItem, ttlMs);
			return CompletableFuture.completedFuture(StepResult.proceed(context));
		}

		if (credentialService.register(
				providerSubject,
				candidate.getPasswordHash(),
				candidate.getHashingMethod(),
				PasswordChangeReason.REGISTER
		).isEmpty()) {
			return CompletableFuture.completedFuture(StepResult.failed(""));
		}

		return CompletableFuture.completedFuture(StepResult.complete(context));
	}

	private long migrationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null)
			return 0L;
		return settings.getConnection().getScenarios().getMigration().pipelineTtlMillis();
	}

	private @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	private @Nullable CredentialRegistrationAttempt consumeRegistrationAttempt(
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

	private @Nullable CredentialRegisterStateItem getRegisterState(@NotNull ScenarioContext context) {
		PipelineState stored = pipelineStateStore.find(reference(context)).orElse(null);
		return stored != null ? stored.item(CredentialRegisterStateItem.class).orElse(null) : null;
	}

	private void storeRegisterState(
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

	private void clearRegisterState(@NotNull ScenarioContext context, long ttlMs) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return;

		stored.removeItem(CredentialRegisterStateItem.class);
		if (ttlMs > 0) {
			pipelineStateStore.save(reference, stored, ttlMs);
		}
	}

	private static @NotNull String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty())
			return "";
		return String.join("\n", lines);
	}
}
