package me.whereareiam.identica.provider.cracked.pipeline.scenario.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegisterStateItem;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedRegistrationAttempt;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import me.whereareiam.identica.provider.cracked.util.PasswordRules;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrackedMigrationRegistrationStep extends InteractiveStep {
	private final Provider<CrackedMessages> messagesProvider;
	private final Provider<CrackedSettings> settingsProvider;
	private final Provider<Settings> coreSettingsProvider;
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final PasswordRules passwordPolicy;
	private final PipelineStateStore pipelineStateStore;

	@Inject
	public CrackedMigrationRegistrationStep(
			Provider<CrackedMessages> messagesProvider,
			Provider<CrackedSettings> settingsProvider,
			Provider<Settings> coreSettingsProvider,
			CrackedAccountService accountService,
			CryptographyService cryptographyService,
			PasswordRules passwordPolicy,
			PipelineStateStore pipelineStateStore
	) {
		super("cracked-migration-registration");
		this.messagesProvider = messagesProvider;
		this.settingsProvider = settingsProvider;
		this.coreSettingsProvider = coreSettingsProvider;
		this.accountService = accountService;
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
		String username = context.getUsername();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		CrackedAccount account = accountService.find(providerSubject).orElse(null);
		if (account != null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		CrackedMessages messages = messagesProvider.get();
		CrackedSettings settings = settingsProvider.get();
		CrackedSettings.Scenario.Registration registrationSettings = settings != null
				&& settings.getScenario() != null
				? settings.getScenario().getRegistration()
				: null;

		if (registrationSettings == null || !registrationSettings.isEnabled())
			return CompletableFuture.completedFuture(StepResult.denied(messages.getScenario().getRegistration().getDisabled()));

		long ttlMs = migrationTtlMs();
		boolean requireRepeat = registrationSettings.isRequireRepeat();
		CrackedRegisterStateItem pending = getRegisterState(context);

		if (requireRepeat && pending != null)
			return CompletableFuture.completedFuture(StepResult.proceed(context));

		if (!requireRepeat && pending != null)
			clearRegisterState(context, ttlMs);

		CrackedRegistrationAttempt input = consumeRegistrationAttempt(context, ttlMs);
		if (input == null)
			return CompletableFuture.completedFuture(StepResult.waiting(joinLines(messages.getScenario().getRegistration().getPrompt())));

		if (input.isConfirm())
			return CompletableFuture.completedFuture(StepResult.waiting(messages.getScenario().getRegistration().getNoPending()));

		String error = passwordPolicy.validate(input.getPassword());
		if (error != null && !error.isBlank())
			return CompletableFuture.completedFuture(StepResult.waiting(error));

		PasswordCandidate candidate = cryptographyService.hash(input.getPassword());
		if (candidate == null)
			return CompletableFuture.completedFuture(StepResult.failed(""));

		if (requireRepeat) {
			CrackedRegisterStateItem stateItem = new CrackedRegisterStateItem(
					candidate.getPasswordHash(),
					candidate.getHashingMethod()
			);
			storeRegisterState(context, stateItem, ttlMs);
			return CompletableFuture.completedFuture(StepResult.proceed(context));
		}

		if (accountService.register(
				providerSubject,
				candidate.getPasswordHash(),
				candidate.getHashingMethod(),
				PasswordChangeReason.REGISTER
		).isEmpty()) {
			return CompletableFuture.completedFuture(StepResult.failed(""));
		}

		return CompletableFuture.completedFuture(complete(context, providerSubject, username));
	}

	private StepResult complete(ScenarioContext context, String providerSubject, String username) {
		ProviderContext provider = ProviderContext.builder()
				.providerId(CrackedConstants.PROVIDER_ID)
				.providerSubject(providerSubject)
				.providerUsername(username == null ? "" : username)
				.build();
		context.setProvider(provider);
		return StepResult.complete(context);
	}

	private String resolveProviderSubject(String username) {
		UUID uuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		return uuid != null ? uuid.toString() : null;
	}

	private long migrationTtlMs() {
		Settings settings = coreSettingsProvider.get();
		if (settings == null)
			return 0L;
		return settings.getConnection().getMigration().pipelineTtlMillis();
	}

	private @NotNull PipelineStateReference reference(@NotNull ScenarioContext context) {
		return PipelineStateReference.from(context);
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

	private @Nullable CrackedRegisterStateItem getRegisterState(@NotNull ScenarioContext context) {
		PipelineState stored = pipelineStateStore.find(reference(context)).orElse(null);
		return stored != null ? stored.item(CrackedRegisterStateItem.class).orElse(null) : null;
	}

	private void storeRegisterState(
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
