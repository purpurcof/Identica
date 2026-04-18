package me.whereareiam.identica.engine.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractJourneyRegistry;
import me.whereareiam.identica.engine.step.EnrollmentStep;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.pipeline.journey.registry.type.RegistrationJourneyRegistry;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.pipeline.journey.StageType;

import java.util.EnumSet;

@Singleton
public class DefaultRegistrationStageRegistry extends AbstractJourneyRegistry implements RegistrationJourneyRegistry {
	@Inject
	public DefaultRegistrationStageRegistry(EnrollmentStep enrollmentStep) {
		registerStep(JourneyStep.builder()
				.stageId(StageType.PRE.id())
				.step(enrollmentStep)
				.order(enrollmentStep.order())
				.scenarios(EnumSet.of(PipelineType.REGISTRATION))
				.flows(EnumSet.of(JourneyType.INTERACTIVE))
				.build());

		PipelineType pipelineType = PipelineType.REGISTRATION;
		registerStage(JourneyStage.builder()
				.id(StageType.PRE.id())
				.type(StageType.PRE)
				.order(100)
				.pipelineTypes(EnumSet.of(pipelineType))
				.flows(EnumSet.allOf(JourneyType.class))
				.allowFallback(true)
				.build());

		registerStage(JourneyStage.builder()
				.id(StageType.PROVIDER.id())
				.type(StageType.PROVIDER)
				.order(200)
				.pipelineTypes(EnumSet.of(pipelineType))
				.flows(EnumSet.allOf(JourneyType.class))
				.requireCompletion(true)
				.usesCompletionResult(true)
				.allowFallback(true)
				.build());

		registerStage(JourneyStage.builder()
				.id(StageType.END.id())
				.type(StageType.END)
				.order(300)
				.pipelineTypes(EnumSet.of(pipelineType))
				.flows(EnumSet.allOf(JourneyType.class))
				.allowFallback(true)
				.build());
	}
}
