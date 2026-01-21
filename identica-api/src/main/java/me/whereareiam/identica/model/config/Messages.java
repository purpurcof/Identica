package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.commandant.model.message.ExceptionMessages;
import me.whereareiam.commandant.model.message.HelpMessages;
import me.whereareiam.commandant.model.message.PaginationMessages;

import java.util.List;

@Getter
@Setter
@ToString
public class Messages {
	private String prefix;
	private Commands commands;
	private Providers providers;
	private Authentication authentication;

	@Getter
	@Setter
	@ToString
	public static class Commands {
		private ExceptionMessages exceptions;
		private PaginationMessages pagination;
		private HelpMessages help;
		private String main;
		private Reload reload;

		/**
		 * Configuration for reload command messages.
		 */
		@Getter
		@Setter
		@ToString
		public static class Reload {
			/**
			 * Success message when reload completes successfully.
			 * Placeholders:
			 * - <prefix>: The global message prefix
			 */
			private String success;

			/**
			 * Error message when reload fails.
			 * Placeholders:
			 * - <prefix>: The global message prefix
			 * - <error>: The error message
			 */
			private String error;
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Providers {
		private List<String> noProvidersAvailable;
		private List<String> noProvidersMatched;
	}

	@Getter
	@Setter
	@ToString
	public static class Authentication {
		private List<String> handshakeDenied;
		private List<String> authenticationFailed;
		private List<String> noCompletionStep;
		private List<String> stepNoStatus;
	}
}
