package me.whereareiam.identica.engine.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.AuthenticationPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.registration.RegistrationPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.migration.MigrationPipeline;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.ConnectionDecisionEvent;
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
	private final AuthenticationPipeline authenticationPipeline;
	private final RegistrationPipeline registrationPipeline;
	private final MigrationPipeline migrationPipeline;
	private final EventManager eventManager;

	public @NotNull ConnectionDecision resolveDecision(
			@Nullable PipelineResult result,
			@Nullable Throwable error,
			@NotNull PipelineType pipelineType
	) {
		if (error != null) {
			Logger.severe("Connection flow failed %s", error);
			return failureDecision(pipelineType);
		}

		if (result == null)
			return failureDecision(pipelineType);

		AuthContext authContext = resolveAuthContext(result, pipelineType);
		if (authContext != null)
			eventManager.call(new FlowAttemptFinishedEvent(authContext, result));

		ConnectionDecision decision = switch (pipelineType) {
			case REGISTRATION -> registrationPipeline.mapDecision(result);
			case MIGRATION -> migrationPipeline.mapDecision(result);
			case AUTHENTICATION -> authenticationPipeline.mapDecision(result);
		};
		if (authContext == null)
			return decision;

		ConnectionDecisionEvent decisionEvent = new ConnectionDecisionEvent(authContext, decision);
		eventManager.call(decisionEvent);
		ConnectionDecision finalDecision = decisionEvent.getDecision();
		return finalDecision != null ? finalDecision : decision;
	}

	private @Nullable AuthContext resolveAuthContext(@NotNull PipelineResult result, @NotNull PipelineType pipelineType) {
		if (pipelineType != PipelineType.AUTHENTICATION)
			return null;
		return authenticationPipeline.resolveAuthContext(result);
	}

	private @NotNull ConnectionDecision failureDecision(@NotNull PipelineType pipelineType) {
		return switch (pipelineType) {
			case REGISTRATION -> registrationPipeline.failureDecision();
			case MIGRATION -> migrationPipeline.failureDecision();
			case AUTHENTICATION -> authenticationPipeline.failureDecision();
		};
	}
}
