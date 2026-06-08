package me.whereareiam.identica.feature.verification.event.challenge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.feature.verification.VerificationInteraction;
import me.whereareiam.identica.feature.verification.model.challenge.VerificationChallengeResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before the default verification challenge logic is evaluated.
 *
 * <p>Listeners may set a custom result to short-circuit the built-in handler.</p>
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class VerificationChallengeEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @NotNull String providerId;
	private final @NotNull String methodId;
	private final @Nullable VerificationInteraction interaction;
	private @Nullable VerificationChallengeResult<?> result;
}
