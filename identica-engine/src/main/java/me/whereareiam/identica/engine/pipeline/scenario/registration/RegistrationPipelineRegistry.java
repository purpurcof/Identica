package me.whereareiam.identica.engine.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityGroup;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.phase.*;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy.PolicyGroup;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy.phase.AccountReviewPhase;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy.phase.PersistUsernameChangePhase;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.session.SessionGroup;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.session.phase.BuildSessionPhase;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.session.phase.OpenSessionPhase;
import me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractPipelineGroupRegistry;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.finalize.FinalizeGroup;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.JourneyGroup;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase.*;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.preparation.PreparationGroup;
import me.whereareiam.identica.model.pipeline.phase.PhasePlacement;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import org.jetbrains.annotations.NotNull;

@Singleton
public class RegistrationPipelineRegistry extends AbstractPipelineGroupRegistry implements PipelineRegistry {
	@Inject
	public RegistrationPipelineRegistry(
			PreparationGroup preparationGroup,
			JourneyGroup journeyGroup,
			FinalizeGroup finalizeGroup,
			IdentityGroup identityGroup,
			PolicyGroup policyGroup,
			SessionGroup sessionGroup,
			LoadContextPhase loadContextPhase,
			ResolveJourneyModePhase resolveJourneyModePhase,
			BuildExecutionPlanPhase buildExecutionPlanPhase,
			ApplyRulesPhase applyRulesPhase,
			ExecutePlanPhase executePlanPhase,
			ValidateProviderPhase validateProviderPhase,
			EnforceProviderJoinRestrictionPhase enforceProviderJoinRestrictionPhase,
			EnsureNewAccountPhase ensureNewAccountPhase,
			CreateAccountPhase createAccountPhase,
			LinkProviderPhase linkProviderPhase,
			UsernameReplicationPhase usernameReplicationPhase,
			AccountReviewPhase accountReviewPhase,
			PersistUsernameChangePhase persistUsernameChangePhase,
			BuildSessionPhase buildSessionPhase,
			OpenSessionPhase openSessionPhase
	) {
		register(preparationGroup);
		register(journeyGroup);
		register(identityGroup);
		register(policyGroup);
		register(sessionGroup);
		register(finalizeGroup);

		registerPhase(journeyGroup.id(), loadContextPhase, PhasePlacement.first());
		registerPhase(journeyGroup.id(), resolveJourneyModePhase, PhasePlacement.after(loadContextPhase.id()));
		registerPhase(journeyGroup.id(), buildExecutionPlanPhase, PhasePlacement.after(resolveJourneyModePhase.id()));
		registerPhase(journeyGroup.id(), applyRulesPhase, PhasePlacement.after(buildExecutionPlanPhase.id()));
		registerPhase(journeyGroup.id(), executePlanPhase, PhasePlacement.last());

		registerPhase(identityGroup.id(), validateProviderPhase, PhasePlacement.first());
		registerPhase(identityGroup.id(), enforceProviderJoinRestrictionPhase, PhasePlacement.after(validateProviderPhase.id()));
		registerPhase(identityGroup.id(), ensureNewAccountPhase, PhasePlacement.after(enforceProviderJoinRestrictionPhase.id()));
		registerPhase(identityGroup.id(), createAccountPhase, PhasePlacement.after(ensureNewAccountPhase.id()));
		registerPhase(identityGroup.id(), linkProviderPhase, PhasePlacement.after(createAccountPhase.id()));
		registerPhase(identityGroup.id(), usernameReplicationPhase, PhasePlacement.last());

		registerPhase(policyGroup.id(), accountReviewPhase, PhasePlacement.first());
		registerPhase(policyGroup.id(), persistUsernameChangePhase, PhasePlacement.last());

		registerPhase(sessionGroup.id(), buildSessionPhase, PhasePlacement.first());
		registerPhase(sessionGroup.id(), openSessionPhase, PhasePlacement.last());
	}

	@Override
	protected @NotNull String phaseLabel() {
		return "registration pipeline phase";
	}
}
