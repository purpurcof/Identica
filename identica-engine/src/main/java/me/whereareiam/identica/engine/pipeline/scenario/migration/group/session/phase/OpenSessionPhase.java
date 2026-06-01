package me.whereareiam.identica.engine.pipeline.scenario.migration.group.session.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.base.session.phase.base.AbstractOpenSessionPhase;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.state.scenario.migration.SessionState;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class OpenSessionPhase extends AbstractOpenSessionPhase<MigrationContext, SessionState> {
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
		return PipelineType.MIGRATION;
	}

	@Override
	protected @NotNull String failedMessage(@NotNull Messages messages) {
		return joinMessage(messages.getScenarios().getMigration().getMigrationFailed());
	}

	@Override
	protected MigrationContext resolveContext(@NotNull SessionState state) {
		return state.getContext();
	}

	@Override
	protected Session resolveSession(@NotNull SessionState state) {
		return state.getSession();
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
