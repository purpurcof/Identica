package me.whereareiam.identica.event.scenario;

import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.ScenarioResolution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base marker for scenario events fired when a player leaves a held scenario
 * state.
 */
public interface ScenarioResolvedEvent extends Event {
	/**
	 * Returns the connection unique id for the scenario holder.
	 *
	 * @return connection unique id
	 */
	@NotNull UUID getConnectionUniqueId();

	/**
	 * Returns the resolved account unique id when it is already known.
	 *
	 * @return resolved account unique id or {@code null}
	 */
	@Nullable UUID getAccountUniqueId();

	/**
	 * Returns the concrete scenario context that left the pending state.
	 *
	 * @return scenario context
	 */
	@NotNull ScenarioContext getContext();

	/**
	 * Returns the reason why the held scenario state ended.
	 *
	 * @return scenario resolution reason
	 */
	@NotNull ScenarioResolution getReason();

	/**
	 * Returns whether leaving the scenario also opened a live session.
	 *
	 * @return {@code true} when a session was opened
	 */
	boolean isSessionOpened();
}
