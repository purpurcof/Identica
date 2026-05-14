package me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityState;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.migration.MigrationPendingState;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.provider.ProviderCapability;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ValidateTargetPhase implements PipelinePhase<IdentityState> {
	private final ProviderManager providerManager;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "validate-target";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<IdentityState> stateType() {
		return IdentityState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<IdentityState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull IdentityState state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		MigrationContext context = resolveContext(pipelineState);
		if (context == null) {
			state.setResult(PipelineResult.failed(providerValidationMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String targetProviderId = resolveTargetProviderId(context, pipelineState);
		if (targetProviderId == null || targetProviderId.isBlank()) {
			state.setResult(PipelineResult.failed(providerValidationMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		if (!supportsMigration(targetProviderId)) {
			state.setResult(PipelineResult.denied(providerValidationMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		context.setTargetProviderId(targetProviderId);
		pipelineState.setScenario(context);
		state.setContext(context);

		Account account = state.getAccount();
		UUID accountId = account != null ? account.getUniqueId() : context.getAccountUniqueId();
		if (accountId == null) {
			state.setResult(PipelineResult.failed(accountMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		AccountProviderLink existing = providerLinkPersistenceService
				.findByUniqueIdAndProviderId(accountId, targetProviderId)
				.orElse(null);
		if (existing != null && existing.isPrimaryLink()) {
			state.setResult(PipelineResult.denied(migrationFailedMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private boolean supportsMigration(@NotNull String providerId) {
		for (InternalProvider provider : providerManager.findProviders(ProviderCapability.MIGRATION)) {
			if (provider == null || provider.getDescriptor() == null) continue;

			String id = provider.getDescriptor().getId();
			if (id.equalsIgnoreCase(providerId)) return true;
		}
		return false;
	}

	private String resolveTargetProviderId(
			@NotNull MigrationContext context,
			@NotNull PipelineState pipelineState
	) {
		String targetProviderId = context.getTargetProviderId();
		if (targetProviderId != null && !targetProviderId.isBlank())
			return targetProviderId;

		MigrationPendingState pending = pipelineState.item(MigrationPendingState.class).orElse(null);
		if (pending != null && !pending.getTargetProviderId().isBlank())
			return pending.getTargetProviderId();

		if (context.getProvider() == null) return null;
		return context.getProvider().getProviderId();
	}

	private @NotNull String migrationFailedMessage() {
		return joinMessage(messagesProvider.get().getConnection().getMigration().getMigrationFailed());
	}

	private @NotNull String providerValidationMissingMessage() {
		return joinMessage(messagesProvider.get()
				.getConnection()
				.getMigration()
				.getErrors()
				.getIdentity().getProvider().getValidationMissing());
	}

	private @NotNull String accountMissingMessage() {
		return joinMessage(messagesProvider.get()
				.getConnection()
				.getMigration()
				.getErrors()
				.getIdentity().getAccountMissing());
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

	private MigrationContext resolveContext(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null) return null;

		return pipelineState.getScenario(pipelineType) instanceof MigrationContext migrationContext
				? migrationContext
				: null;
	}
}
