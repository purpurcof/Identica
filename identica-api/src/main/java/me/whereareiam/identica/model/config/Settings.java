package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.identica.model.Event;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root settings configuration model.
 */
@Getter
@Setter
@ToString
public class Settings {
	private @NotNull Routing routing;
	private @NotNull Synchronization synchronization;
	private @NotNull Sessions sessions;
	private @NotNull Authentication authentication;
	private @NotNull Listeners listeners;

	@Getter
	@Setter
	@ToString
	public static class Routing {
		@Field(name = "default")
		private @NotNull Defaults defaults;
		private @NotNull Map<String, Map<String, String>> providers = new HashMap<>();
		private @NotNull Intent intent;

		@Getter
		@Setter
		@ToString
		public static class Defaults {
			private @NotNull String completedTarget;
			private @NotNull String fallback;
		}

		@Getter
		@Setter
		@ToString
		public static class Intent {
			private @NotNull String mode;
			private @NotNull List<String> allowedServers = new ArrayList<>();
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Synchronization {
		private boolean enabled;
		private @NotNull String serverId;
		private @NotNull Redis redis;

		@Getter
		@Setter
		@ToString
		public static class Redis {
			private @NotNull String host;
			private int port;

			private @NotNull String password;

			private boolean ssl;
			private int timeout;

			private @NotNull Channels channels;
			private @NotNull String sessionsIndexKey;
		}

		@Getter
		@Setter
		@ToString
		public static class Channels {
			private @NotNull String accountUpdates;
			private @NotNull String sessions;
			private @NotNull String conflicts;
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Sessions {
		private @NotNull Duration defaultTtlMinutes;
		private @NotNull Duration refreshMinutes;
		private @NotNull Map<String, Duration> providers = new HashMap<>();
	}

	@Getter
	@Setter
	@ToString
	public static class Authentication {
		private @NotNull Duration handshakeInstructionTtlMinutes;
		private @NotNull Duration pendingUuidTtlMinutes;
	}

	@Getter
	@Setter
	@ToString
	public static class Listeners {
		private @NotNull Map<String, Event> events;
	}
}
