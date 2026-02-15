package me.whereareiam.identica.event.pipeline.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import org.jetbrains.annotations.NotNull;

@Getter
@ToString
@RequiredArgsConstructor
public class PipelineStateSavedEvent implements Event, SynchronousEvent {
	private final @NotNull PipelineStateReference reference;
	private final @NotNull PipelineState state;
	private final long expiresAt;
}
