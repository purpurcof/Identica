package me.whereareiam.identica.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.type.RoutingTargetType;

/**
 * Routing target resolved from routing configuration.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingTarget {
	private final RoutingTargetType type;
	private final String server;
	private final String providerId;
	private final String stepName;
}
