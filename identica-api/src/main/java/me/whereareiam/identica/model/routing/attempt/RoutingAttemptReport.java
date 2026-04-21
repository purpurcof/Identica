package me.whereareiam.identica.model.routing.attempt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Platform report for a routing attempt.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingAttemptReport {
	private final @NotNull UUID connectionUniqueId;
	private final @NotNull RoutingAttemptTrigger trigger;
	private final boolean accepted;
	private final @Nullable String server;
	private final @Nullable String message;
}
