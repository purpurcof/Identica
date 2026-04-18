package me.whereareiam.identica.logging;

public final class Logger {
	private static LoggingHelper loggingHelper;

	public static void init(LoggingHelper loggingHelper) {
		Logger.loggingHelper = loggingHelper;
	}

	public static void info(String message, Object... objects) {
		if (loggingHelper != null) loggingHelper.info(message, objects);
	}

	public static void warn(String message, Object... objects) {
		if (loggingHelper != null) loggingHelper.warn(message, objects);
	}

	public static void severe(String message, Object... objects) {
		if (loggingHelper != null) loggingHelper.severe(message, objects);
	}

	public static void debug(String message, Object... objects) {
		if (loggingHelper != null) loggingHelper.debug(message, objects);
	}

	public static void trace(String message, Object... objects) {
		if (loggingHelper != null) loggingHelper.trace(message, objects);
	}
}
