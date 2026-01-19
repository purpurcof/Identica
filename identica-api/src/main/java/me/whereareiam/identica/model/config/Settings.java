package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import me.whereareiam.identica.model.Event;

import java.util.HashMap;
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
		private String defaultTarget;
		private String fallback;
		private Map<String, String> steps = new HashMap<>();
	}

	@Getter
	@Setter
	@ToString
	public static class Listeners {
		private Map<String, Event> events;
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
			private long timeout;

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
}
