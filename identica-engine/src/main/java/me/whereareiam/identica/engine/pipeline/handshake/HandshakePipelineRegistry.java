package me.whereareiam.identica.engine.pipeline.handshake;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.handshake.group.HandshakeGroup;
import me.whereareiam.identica.engine.pipeline.handshake.group.phase.EvaluatePoliciesPhase;
import me.whereareiam.identica.engine.pipeline.handshake.group.phase.FinalizeDecisionPhase;
import me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractPipelineGroupRegistry;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.phase.PhasePlacement;
import org.jetbrains.annotations.NotNull;

@Singleton
public class HandshakePipelineRegistry extends AbstractPipelineGroupRegistry implements PipelineRegistry {
	@Inject
	public HandshakePipelineRegistry(
			HandshakeGroup handshakeGroup,
			EvaluatePoliciesPhase evaluatePoliciesPhase,
			FinalizeDecisionPhase finalizeDecisionPhase
	) {
		register(handshakeGroup);

		registerPhase(handshakeGroup.id(), evaluatePoliciesPhase, PhasePlacement.first());
		registerPhase(handshakeGroup.id(), finalizeDecisionPhase, PhasePlacement.last());
	}

	@Override
	protected @NotNull String phaseLabel() {
		return "handshake phase";
	}
}
