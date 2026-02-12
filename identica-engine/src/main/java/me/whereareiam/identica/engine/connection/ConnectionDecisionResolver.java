package me.whereareiam.identica.engine.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.AuthenticationPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.registration.RegistrationPipeline;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.ConnectionDecisionEvent;
import me.whereareiam.identica.event.pipeline.attempt.FlowAttemptFinishedEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConnectionDecisionResolver {
	private final AuthenticationPipeline authenticationPipeline;
	private final RegistrationPipeline registrationPipeline;
	private final EventManager eventManager;

	public @NotNull ConnectionDecision resolveDecision(
			@Nullable PipelineResult result,
			@Nullable Throwable error,
			boolean registration
	) {
		if (error != null) {
			Logger.severe("Connection flow failed %s", error);
			return failureDecision(registration);
		}

		if (result == null)
			return failureDecision(registration);

		AuthContext authContext = resolveAuthContext(result, registration);
		if (authContext != null)
			eventManager.call(new FlowAttemptFinishedEvent(authContext, result));

		ConnectionDecision decision = registration
				? registrationPipeline.mapDecision(result)
				: authenticationPipeline.mapDecision(result);
		if (authContext == null)
			return decision;

		ConnectionDecisionEvent decisionEvent = new ConnectionDecisionEvent(authContext, decision);
		eventManager.call(decisionEvent);
		ConnectionDecision finalDecision = decisionEvent.getDecision();
		return finalDecision != null ? finalDecision : decision;
	}

	private @Nullable AuthContext resolveAuthContext(@NotNull PipelineResult result, boolean registration) {
		if (registration)
			return null;
		return authenticationPipeline.resolveAuthContext(result);
	}

	private @NotNull ConnectionDecision failureDecision(boolean registration) {
		return (registration ? registrationPipeline : authenticationPipeline).failureDecision();
	}
}
