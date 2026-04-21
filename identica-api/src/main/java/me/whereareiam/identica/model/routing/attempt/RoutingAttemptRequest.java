package me.whereareiam.identica.model.routing.attempt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Platform request to attempt the current routing intent.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingAttemptRequest {
	private final @NotNull UUID connectionUniqueId;
	private final @NotNull RoutingAttemptTrigger trigger;
	private final @Nullable String currentServer;
}
