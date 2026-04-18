package me.whereareiam.identica.event.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@ToString
@RequiredArgsConstructor
public class SessionOpenedEvent implements Event, SynchronousEvent {
	private final @NotNull UUID connectionUniqueId;
	private final @NotNull PipelineType pipelineType;
	private final @NotNull Session session;
	private final boolean sessionReused;
}
