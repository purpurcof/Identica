package me.whereareiam.identica.provider.cracked.pipeline.scenario.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegistrationAttempt;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegisterStateItem;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.AbstractCrackedStep;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrackedMigrationConfirmStep extends AbstractCrackedStep {
	private final Provider<CrackedMessages> messagesProvider;
	private final Provider<Settings> coreSettingsProvider;
	private final CrackedAccountService accountService;
	private final PipelineStateStore pipelineStateStore;
	private final CryptographyService cryptographyService;

	@Inject
	public CrackedMigrationConfirmStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			PipelineStateStore pipelineStateStore,
			CryptographyService cryptographyService
	) {
		super("cracked-migration-confirm");
		this.messagesProvider = messagesProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.accountService = accountService;
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

		CrackedMessages messages = messagesProvider.get();
		CrackedRegisterStateItem pending = getRegisterState(context);
		if (pending == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterPrompt(messages)));

		long ttlMs = migrationTtlMs();
		CrackedRegistrationAttempt input = consumeRegistrationAttempt(context, ttlMs);
		if (input == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinConfirmPrompt(messages)));

		if (!input.isConfirm()) {
			clearRegisterState(context, ttlMs);
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterReset(messages)));
		}

		if (!matchesPending(input, pending)) {
			clearRegisterState(context, ttlMs);
			return CompletableFuture.completedFuture(StepResult.waiting(joinRegisterReset(messages)));
		}

		CrackedAccount account = accountService.register(
				providerSubject,
				pending.getPasswordHash(),
				pending.getHashingMethod(),
				PasswordChangeReason.REGISTER
		).orElse(null);
		clearRegisterState(context, ttlMs);
		if (account == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		return CompletableFuture.completedFuture(StepResult.complete(context));
	}

	private boolean matchesPending(CrackedRegistrationAttempt input, CrackedRegisterStateItem pending) {
		if (input == null || pending == null)
			return false;
		return cryptographyService.verify(
				input.getPassword(),
				pending.getPasswordHash(),
				pending.getHashingMethod()
		);
	}

	private long migrationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null)
			return 0L;
		return settings.getConnection().getMigration().pipelineTtlMillis();
	}

	private String joinRegisterPrompt(CrackedMessages messages) {
		return joinLines(messages.getScenario().getRegistration().getPrompt());
	}

	private String joinConfirmPrompt(CrackedMessages messages) {
		return joinLines(messages.getScenario().getRegistration().getConfirmPrompt());
	}

	private String joinRegisterReset(CrackedMessages messages) {
		String mismatch = messages.getScenario().getRegistration().getStatus().getMismatch();
		String prompt = joinRegisterPrompt(messages);
		if (mismatch == null || mismatch.isBlank())
			return prompt;
		return mismatch + "\n" + prompt;
	}

	private @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
	}

	private @Nullable CrackedRegisterStateItem getRegisterState(@NotNull ScenarioContext context) {
		PipelineState stored = pipelineStateStore.find(reference(context)).orElse(null);
		return stored != null ? stored.item(CrackedRegisterStateItem.class).orElse(null) : null;
	}

	private @Nullable CrackedRegistrationAttempt consumeRegistrationAttempt(
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

	private void clearRegisterState(@NotNull ScenarioContext context, long ttlMs) {
		PipelineStateReference reference = reference(context);
		PipelineState stored = pipelineStateStore.find(reference).orElse(null);
		if (stored == null) return;

		stored.removeItem(CrackedRegisterStateItem.class);
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
