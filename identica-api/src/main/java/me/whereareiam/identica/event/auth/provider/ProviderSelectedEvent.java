package me.whereareiam.identica.event.auth.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a provider is selected for an authentication flow.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class ProviderSelectedEvent implements Event, SynchronousEvent {
	private final @NotNull AuthContext context;
	private final @NotNull InternalProvider provider;
	private final @NotNull AuthFlowType flow;
}
