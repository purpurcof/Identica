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
import me.whereareiam.identica.provider.credential.cryptography.CryptographyService;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegisterStateItem;
import me.whereareiam.identica.provider.credential.pipeline.CredentialRegistrationAttempt;
import me.whereareiam.identica.provider.credential.pipeline.scenario.AbstractCredentialStep;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CredentialMigrationConfirmStep extends AbstractCredentialStep {
	private final Provider<CredentialMessages> messagesProvider;
	private final Provider<Settings> coreSettingsProvider;
	private final CredentialAccountService credentialService;
	private final PipelineStateStore pipelineStateStore;
	private final CryptographyService cryptographyService;

	@Inject
	public CredentialMigrationConfirmStep(
			Provider<CredentialMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CredentialAccountService credentialService,
			PipelineStateStore pipelineStateStore,
			CryptographyService cryptographyService
	) {
		super("password-migration-confirm");
		this.messagesProvider = messagesProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.credentialService = credentialService;
		this.pipelineStateStore = pipelineStateStore;
		this.cryptographyService = cryptographyService;
	}

	@Override
	public int order() {
		return 20;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		String providerSubject = requireProviderSubject(context);

		CredentialMessages messages = messagesProvider.get();
		CredentialRegisterStateItem pending = getRegisterState(context);
		if (pending == null) return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterPrompt(messages)));

		long ttlMs = migrationTtlMs();
		CredentialRegistrationAttempt input = consumeRegistrationAttempt(context, ttlMs);
		if (input == null) return CompletableFuture.completedFuture(StepResult.waiting(joinConfirmPrompt(messages)));

		if (!input.isConfirm()) {
			clearRegisterState(context, ttlMs);
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterReset(messages)));
		}

		if (!matchesPending(input, pending)) {
			clearRegisterState(context, ttlMs);
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterReset(messages)));
		}

		CredentialAccount credential = credentialService.register(
				providerSubject,
				pending.getPasswordHash(),
				pending.getHashingMethod(),
				PasswordChangeReason.REGISTER
		).orElse(null);
		clearRegisterState(context, ttlMs);
		if (credential == null) return CompletableFuture.completedFuture(StepResult.failed(""));

		return CompletableFuture.completedFuture(StepResult.complete(context));
	}

	private boolean matchesPending(CredentialRegistrationAttempt input, CredentialRegisterStateItem pending) {
		if (input == null || pending == null) return false;
		return cryptographyService.verify(
				input.getPassword(),
				pending.getPasswordHash(),
				pending.getHashingMethod()
		);
	}

	private long migrationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null) return 0L;
		return settings.getConnection().getScenarios().getMigration().pipelineTtlMillis();
	}

	private String joinRegisterPrompt(CredentialMessages messages) {
		return joinLines(messages.getScenario().getMigration().getSetup().getPrompt());
	}

	private String joinConfirmPrompt(CredentialMessages messages) {
		return joinLines(messages.getScenario().getMigration().getSetup().getConfirmPrompt());
	}

	private String joinRegisterReset(CredentialMessages messages) {
		String mismatch = messages.getScenario().getMigration().getSetup().getStatus().getMismatch();
		String prompt = joinRegisterPrompt(messages);
		if (mismatch == null || mismatch.isBlank())
			return prompt;

		return mismatch + "\n" + prompt;
	}

	private @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	private @Nullable CredentialRegisterStateItem getRegisterState(@NotNull ScenarioContext context) {
		PipelineState stored = pipelineStateStore.find(reference(context)).orElse(null);
		return stored != null ? stored.item(CredentialRegisterStateItem.class).orElse(null) : null;
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
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}

}
