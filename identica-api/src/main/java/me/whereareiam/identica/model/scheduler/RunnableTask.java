package me.whereareiam.identica.model.scheduler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a scheduled task identified by a {@link JobKey}.
 */
@Getter
@ToString
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class RunnableTask {
	/**
	 * Unique key for the task.
	 */
	private final JobKey key;

	/**
	 * The actual task to be executed.
	 */
	private final Runnable runnable;
}
