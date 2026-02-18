package me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityState;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EnsureNewAccountPhase implements PipelinePhase<IdentityState> {
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "ensure-new-account";
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

		RegistrationContext context = state.getContext();
		if (context == null || context.getProvider() == null) {
			state.setResult(PipelineResult.failed(ensureNewAccountMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		ProviderContext provider = context.getProvider();
		if (provider.getProviderId() == null || provider.getProviderId().isBlank()
				|| provider.getProviderSubject() == null || provider.getProviderSubject().isBlank()) {
			state.setResult(PipelineResult.failed(ensureNewAccountMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}
		if (providerLinkPersistenceService.findBySubject(provider.getProviderId(), provider.getProviderSubject()).isPresent()) {
			state.setResult(PipelineResult.denied(accountAlreadyExistsMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		UUID reservedId = context.getIdenticaUniqueId();
		if (reservedId != null && accountPersistenceService.findByUniqueId(reservedId).isPresent()) {
			state.setResult(PipelineResult.denied(accountAlreadyExistsMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String ensureNewAccountMissingMessage() {
		return joinMessage(messagesProvider.get()
				.getConnection()
				.getRegistration()
				.getErrors()
				.getPolicy().getEnsureNewAccountMissing());
	}

	private @NotNull String accountAlreadyExistsMessage() {
		return joinMessage(messagesProvider.get().getConnection().getRegistration().getAccountAlreadyExists());
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
