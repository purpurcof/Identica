package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.CommandDefinition;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for command definitions and command-scoped behavior settings.
 */
@Getter
@Setter
@ToString
public class Commands {
	private @NotNull Behavior behavior;
	private @NotNull Map<String, CommandDefinition> commands = new HashMap<>();

	/**
	 * Configuration for command-specific behavior.
	 */
	@Getter
	@Setter
	@ToString
	public static class Behavior {
		private @NotNull Suggestions suggestions;
		private @NotNull Clear clear;
		private @NotNull Sessions sessions;
		private @NotNull Migration migration;
		/**
		 * Whether Velocity should register commands through Brigadier.
		 */
		private boolean useBrigadier;

		/**
		 * Configuration for command suggestions.
		 */
		@Getter
		@Setter
		@ToString
		public static class Suggestions {
			/**
			 * Maximum number of player suggestions to return.
			 */
			private int playerLimit;
		}

		/**
		 * Configuration for clear command behavior.
		 */
		@Getter
		@Setter
		@ToString
		public static class Clear {
			/**
			 * Time window for confirming a clear request.
			 */
			private @NotNull Duration confirmTtl;
		}

		/**
		 * Configuration for session command behavior.
		 */
		@Getter
		@Setter
		@ToString
		public static class Sessions {
			/**
			 * Page size for session listings.
			 */
			private int listPageSize;
		}

		/**
		 * Configuration for migration command behavior.
		 */
		@Getter
		@Setter
		@ToString
		public static class Migration {
			/**
			 * Time window for confirming a migration request.
			 */
			private @NotNull Duration confirmTtl;
		}
	}
}
