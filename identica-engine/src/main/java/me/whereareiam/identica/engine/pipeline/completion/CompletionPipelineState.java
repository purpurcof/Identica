package me.whereareiam.identica.engine.pipeline.completion;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@ToString
@Builder
public class CompletionPipelineState {
	private @NotNull Identity identity;
	private @NotNull CompletionPendingState pendingState;
	private @NotNull PipelineType pipelineType;
	private @Nullable Session session;
	private @Nullable InternalProvider provider;
	private boolean sessionReused;
	private @Nullable CompletionContext context;
}
