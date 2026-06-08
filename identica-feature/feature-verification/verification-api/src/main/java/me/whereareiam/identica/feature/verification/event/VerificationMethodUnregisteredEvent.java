package me.whereareiam.identica.feature.verification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.feature.verification.model.VerificationMethodDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a verification method is unregistered.
 */
@Getter
@ToString
@AllArgsConstructor
public class VerificationMethodUnregisteredEvent implements Event, SynchronousEvent {
	private final @NotNull VerificationMethodDescriptor descriptor;
}
