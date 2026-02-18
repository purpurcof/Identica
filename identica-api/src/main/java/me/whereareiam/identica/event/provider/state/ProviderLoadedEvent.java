package me.whereareiam.identica.event.provider.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.provider.InternalProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a provider is loaded successfully.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class ProviderLoadedEvent implements Event, SynchronousEvent {
	private final @NotNull InternalProvider provider;
}
