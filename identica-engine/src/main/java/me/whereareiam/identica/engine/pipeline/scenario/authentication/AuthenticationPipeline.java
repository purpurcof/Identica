package me.whereareiam.identica.engine.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractScenarioPipeline;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class AuthenticationPipeline extends AbstractScenarioPipeline {
	@Inject
	public AuthenticationPipeline(
			@Named("authenticationPipelineRegistry") PipelineRegistry flowGroupRegistry,
			Provider<Messages> messagesProvider,
			Provider<Settings> settingsProvider,
			PipelineStateStore pipelineStateStore
	) {
		super(flowGroupRegistry, messagesProvider, settingsProvider, pipelineStateStore, PipelineType.AUTHENTICATION);
	}

	public @Nullable AuthContext resolveAuthContext(@NotNull PipelineResult result) {
		var scenario = resolveScenarioContext(result);
		return scenario instanceof AuthContext authContext ? authContext : null;
	}
}
