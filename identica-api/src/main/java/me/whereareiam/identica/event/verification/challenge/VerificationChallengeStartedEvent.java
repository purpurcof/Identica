package me.whereareiam.identica.event.verification.challenge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a verification challenge is created.
 */
@Getter
@ToString
@AllArgsConstructor
public class VerificationChallengeStartedEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @NotNull String providerId;
	private final @NotNull String methodId;
	private final @NotNull String challengeId;
}
