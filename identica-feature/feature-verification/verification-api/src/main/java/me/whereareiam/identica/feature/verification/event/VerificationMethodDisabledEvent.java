package me.whereareiam.identica.feature.verification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after an enrolled verification method is disabled for an identity.
 */
@Getter
@ToString
@AllArgsConstructor
public class VerificationMethodDisabledEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @NotNull String methodId;
}
