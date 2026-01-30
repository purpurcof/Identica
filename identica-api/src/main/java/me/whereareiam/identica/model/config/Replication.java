package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Replication configuration used for cross-instance synchronization.
 */
@Getter
@Setter
@ToString
public class Replication {
	private boolean enabled;
	private @NotNull String serverId;
	private @NotNull Redis redis;
	private @NotNull Cache cache;

	/**
	 * Redis settings used by the replication adapter.
	 */
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
	}

	/**
	 * Cache namespaces used by synchronized caches.
	 */
	@Getter
	@Setter
	@ToString
	public static class Cache {
		private @NotNull String reservations;
		private @NotNull String instructions;
		private @NotNull Sessions sessions;
		private @NotNull String premiumProfile;
		/**
		 * Cache namespace for pending connection/authentication state.
		 */
		private @NotNull String pendingConnections;
	}

	/**
	 * Session cache namespaces.
	 */
	@Getter
	@Setter
	@ToString
	public static class Sessions {
		private @NotNull String user;
		private @NotNull String session;
		private @NotNull String subject;
	}

	/**
	 * Channel names used for replication events.
	 */
	@Getter
	@Setter
	@ToString
	public static class Channels {
		private @NotNull String accountUpdates;
		private @NotNull String sessions;
		private @NotNull String conflicts;
	}
}
