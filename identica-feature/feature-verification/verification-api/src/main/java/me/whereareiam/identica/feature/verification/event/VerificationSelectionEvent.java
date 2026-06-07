package me.whereareiam.identica.feature.verification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.CancellableEvent;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired before a verification method is selected for a provider.
 *
 * <p>Listeners may cancel the event or rewrite the target provider and method
 * ids before the selection is persisted.</p>
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class VerificationSelectionEvent implements Event, SynchronousEvent, CancellableEvent {
	private final @NotNull UUID uniqueId;
	private @NotNull String providerId;
	private @NotNull String methodId;
	private boolean cancelled;
}
