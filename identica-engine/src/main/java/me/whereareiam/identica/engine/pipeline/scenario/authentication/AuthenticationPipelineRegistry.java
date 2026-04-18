package me.whereareiam.identica.engine.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.IdentityGroup;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.phase.LoadIdentityProfilePhase;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.phase.RefreshProviderProfilePhase;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.phase.ResolveIdentityPhase;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.phase.UsernameReplicationPhase;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.policy.PolicyGroup;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.policy.phase.AccountReviewPhase;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.policy.phase.PersistUsernameChangePhase;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.SessionGroup;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.phase.BuildSessionPhase;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.phase.OpenSessionPhase;
import me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractPipelineGroupRegistry;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.finalize.FinalizeGroup;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.preparation.PreparationGroup;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.JourneyGroup;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase.ApplyRulesPhase;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase.BuildExecutionPlanPhase;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase.ExecutePlanPhase;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase.LoadContextPhase;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase.ResolveFlowPhase;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.model.pipeline.phase.PhasePlacement;
import org.jetbrains.annotations.NotNull;

@Singleton
public class AuthenticationPipelineRegistry extends AbstractPipelineGroupRegistry implements PipelineRegistry {
	@Inject
	public AuthenticationPipelineRegistry(
			PreparationGroup preparationGroup,
			JourneyGroup journeyGroup,
			FinalizeGroup finalizeGroup,
			IdentityGroup identityGroup,
			PolicyGroup policyGroup,
			SessionGroup sessionGroup,
			LoadContextPhase loadContextPhase,
			ResolveFlowPhase resolveFlowPhase,
			BuildExecutionPlanPhase buildExecutionPlanPhase,
			ApplyRulesPhase applyRulesPhase,
			ExecutePlanPhase executePlanPhase,
			ResolveIdentityPhase resolveIdentityPhase,
			LoadIdentityProfilePhase loadIdentityProfilePhase,
			RefreshProviderProfilePhase refreshProviderProfilePhase,
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
		registerPhase(journeyGroup.id(), resolveFlowPhase, PhasePlacement.after(loadContextPhase.id()));
		registerPhase(journeyGroup.id(), buildExecutionPlanPhase, PhasePlacement.after(resolveFlowPhase.id()));
		registerPhase(journeyGroup.id(), applyRulesPhase, PhasePlacement.after(buildExecutionPlanPhase.id()));
		registerPhase(journeyGroup.id(), executePlanPhase, PhasePlacement.last());

		registerPhase(identityGroup.id(), resolveIdentityPhase, PhasePlacement.first());
		registerPhase(identityGroup.id(), loadIdentityProfilePhase, PhasePlacement.after(resolveIdentityPhase.id()));
		registerPhase(identityGroup.id(), refreshProviderProfilePhase, PhasePlacement.after(loadIdentityProfilePhase.id()));
		registerPhase(identityGroup.id(), usernameReplicationPhase, PhasePlacement.last());

		registerPhase(policyGroup.id(), accountReviewPhase, PhasePlacement.first());
		registerPhase(policyGroup.id(), persistUsernameChangePhase, PhasePlacement.last());

		registerPhase(sessionGroup.id(), buildSessionPhase, PhasePlacement.first());
		registerPhase(sessionGroup.id(), openSessionPhase, PhasePlacement.last());
	}

	@Override
	protected @NotNull String phaseLabel() {
		return "authentication flow phase";
	}
}
