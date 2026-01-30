package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Root settings configuration model.
 */
@Getter
@Setter
@ToString
public class Settings {
	private @NotNull Routing routing;
	private @NotNull Sessions sessions;
	private @NotNull Authentication authentication;
	private @NotNull Listeners listeners;

	@Getter
	@Setter
	@ToString
	public static class Routing {
		private @NotNull Targets targets;
		private @NotNull Overrides overrides;

		/**
		 * Routing targets by phase.
		 */
		@Getter
		@Setter
		@ToString
		public static class Targets {
			private @NotNull String pre;
			private @NotNull String provider;
			private @NotNull String end;
			private @NotNull String completed;
		}

		/**
		 * Routing overrides.
		 */
		@Getter
		@Setter
		@ToString
		public static class Overrides {
			private @NotNull Map<String, String> steps = new HashMap<>();
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Sessions {
		private @NotNull Duration defaultTtl;
		private @NotNull Duration refreshTtl;
		private @NotNull Map<String, Duration> providers = new HashMap<>();
	}

	@Getter
	@Setter
	@ToString
	public static class Authentication {
		private @NotNull Duration handshakeInstructionTtl;
		private @NotNull Duration reservationTtl;
		private @NotNull AuthFlowType flow;
	}

	@Getter
	@Setter
	@ToString
	public static class Listeners {
		private @NotNull Map<String, Event> events;
	}
}
