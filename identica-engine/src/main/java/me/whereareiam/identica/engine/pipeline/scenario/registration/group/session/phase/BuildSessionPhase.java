package me.whereareiam.identica.engine.pipeline.scenario.registration.group.session.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.session.SessionState;
import me.whereareiam.identica.engine.pipeline.scenario.base.session.phase.base.AbstractBuildSessionPhase;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class BuildSessionPhase extends AbstractBuildSessionPhase<RegistrationContext, SessionState> {
	@Inject
	public BuildSessionPhase(Provider<Messages> messagesProvider) {
		super(messagesProvider);
	}

	@Override
	public @NotNull Class<SessionState> stateType() {
		return SessionState.class;
	}

	@Override
	protected @Nullable RegistrationContext resolveContext(
			@NotNull SessionState state,
			@NotNull PipelineState pipelineState
	) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null)
			return null;
		return pipelineState.getScenario(pipelineType) instanceof RegistrationContext registrationContext
				? registrationContext
				: null;
	}

	@Override
	protected void storeContext(@NotNull SessionState state, @NotNull RegistrationContext context) {
		state.setContext(context);
	}

	@Override
	protected void storeSession(@NotNull SessionState state, @NotNull Session session) {
		state.setSession(session);
	}

	@Override
	protected @NotNull String sessionBuildMissingMessage(@NotNull Messages messages) {
		return joinMessage(messages.getScenarios()
				.getRegistration()
				.getErrors()
				.getSession().getBuildMissing());
	}

	private @NotNull String joinMessage(@NotNull java.util.List<String> lines) {
		return String.join("\n", lines);
	}
}
