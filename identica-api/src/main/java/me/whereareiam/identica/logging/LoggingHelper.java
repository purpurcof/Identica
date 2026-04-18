package me.whereareiam.identica.logging;

/**
 * Interface for handling logging operations in the Identica plugin.
 * Provides methods for logging messages at different severity levels with
 * support for message formatting.
 * <p>
 * Each logging method accepts a message template and optional objects for
 * formatting the message using placeholder substitution.
 */
@SuppressWarnings("unused")
public interface LoggingHelper {
	/**
	 * Logs an informational message.
	 *
	 * @param message the message template to log
	 * @param objects optional objects to format into the message
	 */
	void info(String message, Object... objects);

	/**
	 * Logs a warning message.
	 *
	 * @param message the message template to log
	 * @param objects optional objects to format into the message
	 */
	void warn(String message, Object... objects);

	/**
	 * Logs a severe error message.
	 *
	 * @param message the message template to log
	 * @param objects optional objects to format into the message
	 */
	void severe(String message, Object... objects);

	/**
	 * Logs a debug message.
	 * These messages are typically only shown when debug mode is enabled.
	 *
	 * @param message the message template to log
	 * @param objects optional objects to format into the message
	 */
	void debug(String message, Object... objects);

	/**
	 * Logs a trace message.
	 * These messages are intended for the most verbose diagnostics.
	 *
	 * @param message the message template to log
	 * @param objects optional objects to format into the message
	 */
	void trace(String message, Object... objects);
}
