package me.whereareiam.identica.event.verification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.verification.VerificationMethodDescriptor;
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
