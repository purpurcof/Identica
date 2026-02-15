package me.whereareiam.identica.model.scheduler;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a runnable task that executes after a specified delay.
 */
@Getter
@ToString
@SuperBuilder(toBuilder = true)
public class DelayedRunnableTask extends RunnableTask {
	/**
	 * The delay in milliseconds before the task should be executed.
	 */
	private final long delay;
}
