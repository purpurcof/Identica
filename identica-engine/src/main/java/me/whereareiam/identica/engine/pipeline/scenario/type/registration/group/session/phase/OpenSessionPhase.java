package me.whereareiam.identica.engine.pipeline.scenario.type.registration.group.session.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.session.phase.base.AbstractOpenSessionPhase;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.state.scenario.type.registration.SessionState;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

@Singleton
public class OpenSessionPhase extends AbstractOpenSessionPhase<RegistrationContext, SessionState> {
	@Inject
	public OpenSessionPhase(
			SessionService sessionService,
			Provider<Messages> messagesProvider,
			EventManager eventManager
	) {
		super(sessionService, messagesProvider, eventManager);
	}

	@Override
	public @NotNull Class<SessionState> stateType() {
		return SessionState.class;
	}

	@Override
	protected @NotNull PipelineType pipelineType() {
		return PipelineType.REGISTRATION;
	}

	@Override
	protected @NotNull String failedMessage(@NotNull Messages messages) {
		return joinMessage(messages.getScenarios().getRegistration().getRegistrationFailed());
	}

	@Override
	protected RegistrationContext resolveContext(@NotNull SessionState state) {
		return state.getContext();
	}

	@Override
	protected Session resolveSession(@NotNull SessionState state) {
		return state.getSession();
	}

	private @NotNull String joinMessage(@NotNull java.util.List<String> lines) {
		return String.join("\n", lines);
	}
}
