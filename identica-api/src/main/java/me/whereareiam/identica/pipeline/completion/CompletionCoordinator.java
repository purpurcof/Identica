package me.whereareiam.identica.pipeline.completion;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

public interface CompletionCoordinator {
	void consumeAndExecute(@NotNull Identity identity);

	void execute(@NotNull Identity identity, @NotNull CompletionPendingState pendingState);

	default void execute(
			@NotNull Identity identity,
			@NotNull PipelineType pipelineType,
			@NotNull Session session
	) {
		execute(identity, pipelineType, session, false);
	}

	void execute(
			@NotNull Identity identity,
			@NotNull PipelineType pipelineType,
			@NotNull Session session,
			boolean sessionReused
	);
}
