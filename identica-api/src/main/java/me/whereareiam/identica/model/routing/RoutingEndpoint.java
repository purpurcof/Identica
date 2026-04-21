package me.whereareiam.identica.model.routing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Concrete platform route destination.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingEndpoint {
	private final @NotNull String server;
}
