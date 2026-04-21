package me.whereareiam.identica.model.routing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.type.routing.RoutingClearReason;
import me.whereareiam.identica.type.routing.RoutingPlanAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Planned routing state transition.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingPlan {
	private final @NotNull RoutingPlanAction action;
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable RoutingIntent intent;
	private final @Nullable RoutingClearReason clearReason;

	public static @NotNull RoutingPlan start(@NotNull RoutingIntent intent) {
		return new RoutingPlan(RoutingPlanAction.START, intent.getConnectionUniqueId(), intent, null);
	}

	public static @NotNull RoutingPlan replace(@NotNull RoutingIntent intent) {
		return new RoutingPlan(RoutingPlanAction.REPLACE, intent.getConnectionUniqueId(), intent, null);
	}

	public static @NotNull RoutingPlan clear(@NotNull UUID connectionUniqueId, @NotNull RoutingClearReason reason) {
		return new RoutingPlan(RoutingPlanAction.CLEAR, connectionUniqueId, null, reason);
	}

	public static @NotNull RoutingPlan ignore() {
		return new RoutingPlan(RoutingPlanAction.IGNORE, null, null, null);
	}
}
