package me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy.PolicyState;
import me.whereareiam.identica.engine.pipeline.scenario.base.policy.phase.base.AbstractAccountReviewPhase;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class AccountReviewPhase extends AbstractAccountReviewPhase<RegistrationContext, PolicyState> {
	@Inject
	public AccountReviewPhase(
			AccountPersistenceService accountPersistenceService,
			ProviderLinkPersistenceService providerLinkPersistenceService,
			ProviderProfilePersistenceService providerProfilePersistenceService,
			Provider<Messages> messagesProvider
	) {
		super(
				accountPersistenceService,
				providerLinkPersistenceService,
				providerProfilePersistenceService,
				messagesProvider
		);
	}

	@Override
	public @NotNull Class<PolicyState> stateType() {
		return PolicyState.class;
	}

	@Override
	protected @Nullable RegistrationContext resolveContext(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null)
			return null;
		return pipelineState.getScenario(pipelineType) instanceof RegistrationContext registrationContext
				? registrationContext
				: null;
	}

	@Override
	protected @NotNull String failedMessage(@NotNull Messages messages) {
		return joinMessage(messages.getScenarios().getRegistration().getRegistrationFailed());
	}

	@Override
	protected @NotNull String accountReviewMissingMessage(@NotNull Messages messages) {
		return joinMessage(messages.getScenarios()
				.getRegistration()
				.getErrors()
				.getPolicy().getAccountReviewMissing());
	}
}
