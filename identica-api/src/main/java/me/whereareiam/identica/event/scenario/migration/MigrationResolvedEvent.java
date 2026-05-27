package me.whereareiam.identica.event.scenario.migration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.type.ScenarioResolution;
import me.whereareiam.identica.event.scenario.ScenarioResolvedEvent;
import me.whereareiam.identica.model.migration.MigrationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when a held migration scenario leaves the pending state.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class MigrationResolvedEvent implements Event, SynchronousEvent, ScenarioResolvedEvent {
	private final @NotNull UUID connectionUniqueId;
	private final @Nullable UUID accountUniqueId;
	private final @NotNull MigrationContext context;
	private final @NotNull ScenarioResolution reason;
	private final boolean sessionOpened;

	public MigrationResolvedEvent(
			@NotNull MigrationContext context,
			@NotNull ScenarioResolution reason,
			boolean sessionOpened
	) {
		this(
				requireConnectionUniqueId(context),
				context.getAccountUniqueId(),
				context,
				reason,
				sessionOpened
		);
	}

	private static @NotNull UUID requireConnectionUniqueId(@NotNull MigrationContext context) {
		UUID connectionUniqueId = context.getConnectionUniqueId();
		if (connectionUniqueId == null)
			throw new IllegalArgumentException("Migration context is missing connectionUniqueId");
		return connectionUniqueId;
	}
}
