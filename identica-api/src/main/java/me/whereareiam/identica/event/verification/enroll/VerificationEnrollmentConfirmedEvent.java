package me.whereareiam.identica.event.verification.enroll;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired after a verification enrollment is fully activated.
 *
 * <p>This event is emitted only after any required saved-confirmation step has
 * completed and the enrollment has been persisted.</p>
 */
@Getter
@ToString
@AllArgsConstructor
public class VerificationEnrollmentConfirmedEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @NotNull String methodId;
	private final @Nullable String providerId;
}
