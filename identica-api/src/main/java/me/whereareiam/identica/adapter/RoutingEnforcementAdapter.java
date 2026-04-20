package me.whereareiam.identica.adapter;

import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.type.RoutingTargetType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for applying routing enforcement decisions in a platform-agnostic way.
 */
public abstract class RoutingEnforcementAdapter {
	public final void applyRoutingUpdate(
			@NotNull RoutingUpdateContext context,
			@NotNull RoutingEnforcementTarget target
	) {
		RoutingTarget pendingTarget = context.pendingTarget();
		if (pendingTarget == null) {
			Logger.debug("No pending routing target username=%s", context.username());
			return;
		}

		String targetServer = pendingTarget.getServer();
		if (targetServer == null || targetServer.isBlank()) {
			Logger.debug("Pending routing target blank username=%s type=%s",
					context.username(), pendingTarget.getType());
			return;
		}

		String currentServer = context.currentServer();
		if (currentServer == null) {
			Logger.debug("Skipping routing target application username=%s because no stable current server is available",
					context.username());
			return;
		}

		if (currentServer.equalsIgnoreCase(targetServer)) {
			Logger.debug("Routing already satisfied username=%s current=%s type=%s",
					context.username(), currentServer, pendingTarget.getType());
			return;
		}

		Logger.debug("Applying routing target username=%s current=%s target=%s type=%s",
				context.username(), currentServer, targetServer, pendingTarget.getType());
		target.connect(targetServer);
	}

	public record RoutingUpdateContext(
			@NotNull String username,
			@Nullable String currentServer,
			@Nullable RoutingTarget pendingTarget
	) {
	}

	public interface RoutingEnforcementTarget {
		boolean connect(@NotNull String serverName);

		void consume();
	}
}
