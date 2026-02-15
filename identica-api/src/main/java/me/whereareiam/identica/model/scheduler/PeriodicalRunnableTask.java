package me.whereareiam.identica.model.scheduler;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a task that runs periodically at fixed intervals.
 */
@Getter
@ToString
@SuperBuilder(toBuilder = true)
public class PeriodicalRunnableTask extends RunnableTask {
	/**
	 * The initial delay before the first task execution (in milliseconds).
	 */
	private final long delay;

	/**
	 * The time period between consecutive task executions (in milliseconds).
	 */
	private final long period;
}
