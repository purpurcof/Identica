package me.whereareiam.identica.event.auth.flow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an authentication flow starts.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class AuthFlowStartedEvent implements Event, SynchronousEvent {
	private final @NotNull AuthContext context;
	private final @NotNull AuthFlowType flow;
}
