package me.whereareiam.identica.event.verification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired after verification state is reset for an identity.
 *
 * <p>When {@code fullReset} is {@code true}, all verification state for the
 * identity was cleared. Otherwise, only provider-specific selections for the
 * supplied provider were removed.</p>
 */
@Getter
@ToString
@AllArgsConstructor
public class VerificationResetEvent implements Event, SynchronousEvent {
	private final @NotNull UUID uniqueId;
	private final @Nullable String providerId;
	private final boolean fullReset;
}
