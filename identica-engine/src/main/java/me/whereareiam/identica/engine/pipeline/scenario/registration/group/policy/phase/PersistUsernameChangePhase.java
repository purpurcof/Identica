package me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy.PolicyState;
import me.whereareiam.identica.engine.pipeline.scenario.base.policy.phase.base.AbstractPersistUsernameChangePhase;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class PersistUsernameChangePhase extends AbstractPersistUsernameChangePhase<RegistrationContext, PolicyState> {
	@Inject
	public PersistUsernameChangePhase(
			AccountPersistenceService accountPersistenceService,
			UsernameHistoryPersistenceService usernameHistoryPersistenceService
	) {
		super(accountPersistenceService, usernameHistoryPersistenceService);
	}

	@Override
	public @NotNull Class<PolicyState> stateType() {
		return PolicyState.class;
	}

	@Override
	protected @Nullable RegistrationContext resolveContext(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null)return null;
		return pipelineState.getScenario(pipelineType) instanceof RegistrationContext registrationContext
				? registrationContext
				: null;
	}
}
