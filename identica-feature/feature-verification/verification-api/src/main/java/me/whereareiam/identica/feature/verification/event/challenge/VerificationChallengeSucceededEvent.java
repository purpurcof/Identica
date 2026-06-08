package me.whereareiam.identica.feature.verification.event.challenge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a verification challenge succeeds.
 */
@Getter
@ToString
@AllArgsConstructor
public class VerificationChallengeSucceededEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @NotNull String providerId;
	private final @NotNull String methodId;
	private final boolean recoveryCodeUsed;
}
