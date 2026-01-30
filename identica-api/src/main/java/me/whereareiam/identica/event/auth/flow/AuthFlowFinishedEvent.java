package me.whereareiam.identica.event.auth.flow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when an authentication flow finishes.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class AuthFlowFinishedEvent implements Event, SynchronousEvent {
	private final @NotNull AuthContext context;
	private final @NotNull AuthFlowType flow;
	private final @Nullable StepResult result;
}
