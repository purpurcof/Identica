package me.whereareiam.identica.engine.pipeline.scenario.registration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractScenarioPipeline;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.pipeline.PipelineType;

@Singleton
public class RegistrationPipeline extends AbstractScenarioPipeline {
	@Inject
	public RegistrationPipeline(
			@Named("registrationPipelineRegistry") PipelineRegistry flowGroupRegistry,
			Provider<Messages> messagesProvider,
			Provider<Settings> settingsProvider,
			PipelineStateStore pipelineStateStore
	) {
		super(flowGroupRegistry, messagesProvider, settingsProvider, pipelineStateStore, PipelineType.REGISTRATION);
	}
}
