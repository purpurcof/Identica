package me.whereareiam.identica.common.routing.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.routing.RoutingTargetApplier;

import java.util.Set;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RoutingTargetDispatcher {
	private final Set<RoutingTargetApplier> appliers;

	public void apply(RoutingTarget target, AuthContext context) {
		if (appliers == null || appliers.isEmpty()) return;
		for (RoutingTargetApplier applier : appliers) {
			if (applier == null) continue;
			applier.apply(target, context);
		}
	}
}
