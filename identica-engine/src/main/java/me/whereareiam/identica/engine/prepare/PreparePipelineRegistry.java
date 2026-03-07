package me.whereareiam.identica.engine.prepare;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractPipelineGroupRegistry;
import me.whereareiam.identica.engine.prepare.group.context.ContextGroup;
import me.whereareiam.identica.engine.prepare.group.context.phase.ResolveEntrypointPhase;
import me.whereareiam.identica.engine.prepare.group.context.phase.RestorePrepareStatePhase;
import me.whereareiam.identica.engine.prepare.group.finalize.FinalizeGroup;
import me.whereareiam.identica.engine.prepare.group.finalize.phase.StorePrepareDecisionPhase;
import me.whereareiam.identica.engine.prepare.group.handshake.HandshakeGroup;
import me.whereareiam.identica.engine.prepare.group.handshake.phase.EvaluateHandshakePhase;
import me.whereareiam.identica.engine.prepare.group.handshake.phase.FinalizeHandshakePhase;
import me.whereareiam.identica.engine.prepare.group.policy.PolicyGroup;
import me.whereareiam.identica.engine.prepare.group.policy.phase.ApplyPreparePolicyPhase;
import me.whereareiam.identica.engine.prepare.group.profile.ProfileGroup;
import me.whereareiam.identica.engine.prepare.group.profile.phase.LoadPrepareAccountPhase;
import me.whereareiam.identica.engine.prepare.group.profile.phase.ResolveProfilePhase;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.phase.PhasePlacement;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PreparePipelineRegistry extends AbstractPipelineGroupRegistry implements PipelineRegistry {
	@Inject
	public PreparePipelineRegistry(
			ContextGroup contextGroup,
			HandshakeGroup handshakeGroup,
			ProfileGroup profileGroup,
			PolicyGroup policyGroup,
			FinalizeGroup finalizeGroup,
			RestorePrepareStatePhase restorePrepareStatePhase,
			ResolveEntrypointPhase resolveEntrypointPhase,
			EvaluateHandshakePhase evaluateHandshakePhase,
			FinalizeHandshakePhase finalizeHandshakePhase,
			ResolveProfilePhase resolveProfilePhase,
			LoadPrepareAccountPhase loadPrepareAccountPhase,
			ApplyPreparePolicyPhase applyPreparePolicyPhase,
			StorePrepareDecisionPhase storePrepareDecisionPhase
	) {
		register(contextGroup);
		register(handshakeGroup);
		register(profileGroup);
		register(policyGroup);
		register(finalizeGroup);

		registerPhase(contextGroup.id(), restorePrepareStatePhase, PhasePlacement.first());
		registerPhase(contextGroup.id(), resolveEntrypointPhase, PhasePlacement.last());

		registerPhase(handshakeGroup.id(), evaluateHandshakePhase, PhasePlacement.first());
		registerPhase(handshakeGroup.id(), finalizeHandshakePhase, PhasePlacement.last());

		registerPhase(profileGroup.id(), resolveProfilePhase, PhasePlacement.first());
		registerPhase(profileGroup.id(), loadPrepareAccountPhase, PhasePlacement.last());

		registerPhase(policyGroup.id(), applyPreparePolicyPhase, PhasePlacement.first());

		registerPhase(finalizeGroup.id(), storePrepareDecisionPhase, PhasePlacement.first());
	}

	@Override
	protected @NotNull String phaseLabel() {
		return "prepare flow phase";
	}
}
