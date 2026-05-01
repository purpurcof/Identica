package me.whereareiam.identica.model.routing.attempt;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.type.routing.reason.RoutingAttemptFailureReason;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Platform report for a routing attempt.
 */
@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RoutingAttemptReport {
	private final @NotNull UUID connectionUniqueId;
	private final @NotNull RoutingAttemptTrigger trigger;
	private final boolean accepted;
	private final @Nullable String server;
	private final @Nullable RoutingAttemptFailureReason failureReason;

	public static @NotNull RoutingAttemptReport succeeded(
			@NotNull UUID connectionUniqueId,
			@NotNull RoutingAttemptTrigger trigger,
			@Nullable String server
	) {
		return new RoutingAttemptReport(connectionUniqueId, trigger, true, server, null);
	}

	public static @NotNull RoutingAttemptReport failed(
			@NotNull UUID connectionUniqueId,
			@NotNull RoutingAttemptTrigger trigger,
			@Nullable String server,
			@Nullable RoutingAttemptFailureReason failureReason
	) {
		return new RoutingAttemptReport(connectionUniqueId, trigger, false, server, failureReason);
	}
}
