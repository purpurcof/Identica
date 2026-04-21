package me.whereareiam.identica.model.routing.attempt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.model.routing.RoutingIntent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of checking whether a platform routing attempt may run.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingAttemptDecision {
	private final boolean allowed;
	private final boolean exhausted;
	private final @Nullable RoutingIntent intent;
	private final @NotNull String reason;

	public static @NotNull RoutingAttemptDecision allowed(@NotNull RoutingIntent intent) {
		return new RoutingAttemptDecision(true, false, intent, "allowed");
	}

	public static @NotNull RoutingAttemptDecision skipped(@NotNull String reason) {
		return new RoutingAttemptDecision(false, false, null, reason);
	}

	public static @NotNull RoutingAttemptDecision exhausted(@NotNull RoutingIntent intent) {
		return new RoutingAttemptDecision(false, true, intent, "exhausted");
	}
}
