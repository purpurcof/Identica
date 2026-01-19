package me.whereareiam.identica.platform.velocity.logging;

import com.google.inject.Inject;
import me.whereareiam.identica.logging.LoggingHelper;
import org.slf4j.Logger;

public final class VelocityLoggingHelper implements LoggingHelper {
	private final Logger logger;

	@Inject
	public VelocityLoggingHelper(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void info(String message, Object... objects) {
		if (logger != null) logger.info(format(message, objects));
	}

	@Override
	public void warn(String message, Object... objects) {
		if (logger != null) logger.warn(format(message, objects));
	}

	@Override
	public void severe(String message, Object... objects) {
		if (logger != null) logger.error(format(message, objects));
	}

	@Override
	public void debug(String message, Object... objects) {
		if (logger != null) logger.debug(format(message, objects));
	}

	private String format(String message, Object... objects) {
		if (message == null) return "null";
		return objects == null || objects.length == 0 ? message : String.format(message, objects);
	}
}
