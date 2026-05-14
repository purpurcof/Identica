package me.whereareiam.identica.event.connection.attempt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@ToString
@AllArgsConstructor
public abstract class ConnectionAttemptEvent implements Event, SynchronousEvent {
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable UUID accountUniqueId;
	private final @Nullable String username;
	private final @Nullable String ip;
	private @Nullable ConnectionDecision decision;
}
