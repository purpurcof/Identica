package me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityState;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CreateAccountPhase implements PipelinePhase<IdentityState> {
	private final AccountPersistenceService accountPersistenceService;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "create-account";
	}

	@Override
	public int order() {
		return 300;
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
		AccountProviderProfile profile = state.getProfile();
		if (context == null || profile == null) {
			state.setResult(PipelineResult.failed(accountCreationMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		UUID uniqueId = context.getIdenticaUniqueId();
		if (uniqueId == null)
			uniqueId = UniqueIdGenerator.newIdenticaUniqueId();

		if (accountPersistenceService.findByUniqueId(uniqueId).isPresent()) {
			state.setResult(PipelineResult.denied(accountAlreadyExistsMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		long now = System.currentTimeMillis();
		Account account = Account.builder()
				.uniqueId(uniqueId)
				.username(profile.getProviderUsername())
				.source(UsernameSource.PROVIDER)
				.createdAt(now)
				.lastSeenAt(now)
				.build();
		accountPersistenceService.create(account);

		context.setIdenticaUniqueId(uniqueId);
		pipelineState.setScenario(context);

		state.setAccount(account);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String accountAlreadyExistsMessage() {
		return String.join("\n", messagesProvider.get().getConnection().getRegistration().getAccountAlreadyExists());
	}

	private @NotNull String accountCreationMissingMessage() {
		return String.join("\n", messagesProvider.get()
				.getConnection()
				.getRegistration()
				.getErrors()
				.getPolicy().getAccountCreationMissing());
	}
}
