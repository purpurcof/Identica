package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.policy.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.base.policy.phase.base.AbstractAccountReviewPhase;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.scenario.authentication.PolicyState;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class AccountReviewPhase extends AbstractAccountReviewPhase<AuthContext, PolicyState> {
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
	protected @Nullable AuthContext resolveContext(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null)
			return null;
		return pipelineState.getScenario(pipelineType) instanceof AuthContext authContext
				? authContext
				: null;
	}

	@Override
	protected @NotNull String failedMessage(@NotNull Messages messages) {
		return joinMessage(messages.getScenarios().getAuthentication().getAuthenticationFailed());
	}

	@Override
	protected @NotNull String accountReviewMissingMessage(@NotNull Messages messages) {
		return joinMessage(messages.getScenarios()
				.getAuthentication()
				.getErrors()
				.getPolicy().getAccountReviewMissing());
	}
}
