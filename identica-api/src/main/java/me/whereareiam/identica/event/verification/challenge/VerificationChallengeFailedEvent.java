package me.whereareiam.identica.event.verification.challenge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.type.verification.VerificationChallengeStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a verification challenge fails.
 *
 * <p>This event is intended for post-failure hooks such as logging, metrics,
 * or future challenge-specific lockout policies.</p>
 */
@Getter
@ToString
@AllArgsConstructor
public class VerificationChallengeFailedEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @NotNull String providerId;
	private final @NotNull String methodId;
	private final @NotNull VerificationChallengeStatus status;
}
