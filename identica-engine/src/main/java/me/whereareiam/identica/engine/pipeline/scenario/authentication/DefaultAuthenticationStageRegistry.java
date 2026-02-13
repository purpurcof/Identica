package me.whereareiam.identica.engine.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.journey.AbstractJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.AuthenticationJourneyRegistry;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.pipeline.journey.StageType;

import java.util.EnumSet;

@Singleton
public class DefaultAuthenticationStageRegistry extends AbstractJourneyRegistry implements AuthenticationJourneyRegistry {
	@Inject
	public DefaultAuthenticationStageRegistry() {
		PipelineType pipelineType = PipelineType.AUTHENTICATION;

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
