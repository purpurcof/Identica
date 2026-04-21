package me.whereareiam.identica.event.routing.attempt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptReport;
import me.whereareiam.identica.model.routing.RoutingIntent;
import me.whereareiam.identica.type.routing.RoutingAttemptTrigger;
import org.jetbrains.annotations.NotNull;

@Getter
@ToString
@RequiredArgsConstructor
public class RoutingAttemptFinishedEvent implements RoutingAttemptEvent, SynchronousEvent {
	private final RoutingIntent intent;
	private final RoutingAttemptReport report;

	@Override
	public @NotNull RoutingAttemptTrigger getTrigger() {
		return report.getTrigger();
	}
}
