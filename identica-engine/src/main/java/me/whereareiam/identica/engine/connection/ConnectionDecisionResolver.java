package me.whereareiam.identica.engine.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.AuthenticationPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.ScenarioRegistry;
import me.whereareiam.identica.engine.pipeline.scenario.AbstractScenarioPipeline;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.connection.ConnectionDecisionEvent;
import me.whereareiam.identica.event.pipeline.attempt.FlowAttemptFinishedEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConnectionDecisionResolver {
	private final ScenarioRegistry scenarioRegistry;
	private final EventManager eventManager;

	public @NotNull ConnectionDecision resolveDecision(
			@Nullable PipelineResult result,
			@Nullable Throwable error,
			@NotNull PipelineType pipelineType
	) {
		AbstractScenarioPipeline runner = scenarioRegistry.resolve(pipelineType);
		if (error != null) {
			Logger.severe("Connection flow failed %s", error);
			return failureDecision(runner);
		}

		if (result == null)
			return failureDecision(runner);

		AuthContext authContext = resolveAuthContext(result, pipelineType, runner);
		if (authContext != null)
			eventManager.call(new FlowAttemptFinishedEvent(authContext, result));

		ConnectionDecision decision = runner != null
				? runner.mapDecision(result)
				: ConnectionDecision.deny("Connection failed");
		if (authContext == null)
			return decision;

		ConnectionDecisionEvent decisionEvent = new ConnectionDecisionEvent(authContext, decision);
		eventManager.call(decisionEvent);
		ConnectionDecision finalDecision = decisionEvent.getDecision();
		return finalDecision != null ? finalDecision : decision;
	}

	private @Nullable AuthContext resolveAuthContext(
			@NotNull PipelineResult result,
			@NotNull PipelineType pipelineType,
			@Nullable AbstractScenarioPipeline runner
	) {
		if (pipelineType != PipelineType.AUTHENTICATION)
			return null;
		if (runner instanceof AuthenticationPipeline authenticationPipeline)
			return authenticationPipeline.resolveAuthContext(result);
		return null;
	}

	private @NotNull ConnectionDecision failureDecision(@Nullable AbstractScenarioPipeline runner) {
		if (runner == null) {
			return ConnectionDecision.deny("Connection failed");
		}
		return runner.failureDecision();
	}
}
