package me.whereareiam.identica.engine.pipeline.scenario.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityGroup;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase.ApplyProviderLinkPhase;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase.UsernameReplicationPhase;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase.ResolveAccountPhase;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase.ValidateProviderPhase;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase.ValidateTargetPhase;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.session.SessionGroup;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.session.phase.BuildSessionPhase;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.session.phase.OpenSessionPhase;
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
public class MigrationPipelineRegistry extends AbstractPipelineGroupRegistry implements PipelineRegistry {
	@Inject
	public MigrationPipelineRegistry(
			PreparationGroup preparationGroup,
			JourneyGroup journeyGroup,
			FinalizeGroup finalizeGroup,
			IdentityGroup identityGroup,
			SessionGroup sessionGroup,
			LoadContextPhase loadContextPhase,
			ResolveFlowPhase resolveFlowPhase,
			BuildExecutionPlanPhase buildExecutionPlanPhase,
			ApplyRulesPhase applyRulesPhase,
			ExecutePlanPhase executePlanPhase,
			ResolveAccountPhase resolveAccountPhase,
			ValidateTargetPhase validateTargetPhase,
			ValidateProviderPhase validateProviderPhase,
			ApplyProviderLinkPhase applyProviderLinkPhase,
			UsernameReplicationPhase usernameReplicationPhase,
			BuildSessionPhase buildSessionPhase,
			OpenSessionPhase openSessionPhase
	) {
		register(preparationGroup);
		register(journeyGroup);
		register(identityGroup);
		register(sessionGroup);
		register(finalizeGroup);

		registerPhase(journeyGroup.id(), loadContextPhase, PhasePlacement.first());
		registerPhase(journeyGroup.id(), resolveFlowPhase, PhasePlacement.after(loadContextPhase.id()));
		registerPhase(journeyGroup.id(), buildExecutionPlanPhase, PhasePlacement.after(resolveFlowPhase.id()));
		registerPhase(journeyGroup.id(), applyRulesPhase, PhasePlacement.after(buildExecutionPlanPhase.id()));
		registerPhase(journeyGroup.id(), executePlanPhase, PhasePlacement.last());

		registerPhase(identityGroup.id(), resolveAccountPhase, PhasePlacement.first());
		registerPhase(identityGroup.id(), validateTargetPhase, PhasePlacement.after(resolveAccountPhase.id()));
		registerPhase(identityGroup.id(), validateProviderPhase, PhasePlacement.after(validateTargetPhase.id()));
		registerPhase(identityGroup.id(), applyProviderLinkPhase, PhasePlacement.after(validateProviderPhase.id()));
		registerPhase(identityGroup.id(), usernameReplicationPhase, PhasePlacement.last());

		registerPhase(sessionGroup.id(), buildSessionPhase, PhasePlacement.first());
		registerPhase(sessionGroup.id(), openSessionPhase, PhasePlacement.last());
	}

	@Override
	protected @NotNull String phaseLabel() {
		return "migration flow phase";
	}
}
