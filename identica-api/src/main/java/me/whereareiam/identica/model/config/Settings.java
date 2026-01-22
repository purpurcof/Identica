package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.identica.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class Settings {
	private Routing routing;
	private Synchronization synchronization;
	private Listeners listeners;

	@Getter
	@Setter
	@ToString
	public static class Routing {
		@Field(name = "default")
		private Defaults defaults;
		private Map<String, Map<String, String>> providers = new HashMap<>();
		private Intent intent;

		@Getter
		@Setter
		@ToString
		public static class Defaults {
			private String completedTarget;
			private String fallback;
		}

		@Getter
		@Setter
		@ToString
		public static class Intent {
			private String mode;
			private List<String> allowedServers = new ArrayList<>();
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Synchronization {
		private boolean enabled;
		private String serverId;
		private Redis redis;

		@Getter
		@Setter
		@ToString
		public static class Redis {
			private String host;
			private int port;

			private String password;

			private boolean ssl;
			private int timeout;

			private Channels channels;
		}

		@Getter
		@Setter
		@ToString
		public static class Channels {
			private String accountUpdates;
			private String sessions;
			private String conflicts;
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Listeners {
		private Map<String, Event> events;
	}
}
