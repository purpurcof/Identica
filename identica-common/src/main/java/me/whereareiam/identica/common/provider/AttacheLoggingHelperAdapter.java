package me.whereareiam.identica.common.provider;

import me.whereareiam.attache.LoggingHelper;
import me.whereareiam.attache.type.Level;
import me.whereareiam.identica.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AttacheLoggingHelperAdapter implements LoggingHelper {
	@Override
	public void log(@NotNull Level level, @NotNull String message) {
		log(level, message, null);
	}

	@Override
	public void log(Level level, @NotNull String message, @Nullable Throwable throwable) {
		switch (level) {
			case WARN -> Logger.warn(message);
			case ERROR -> Logger.severe(message);
			case DEBUG -> Logger.debug(message);
			default -> Logger.info(message);
		}

		if (throwable != null) {
			Logger.debug("Dependency load error: %s", throwable.getMessage());
		}
	}
}
