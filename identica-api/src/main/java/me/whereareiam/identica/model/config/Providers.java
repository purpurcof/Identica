package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.annotation.Field;

import java.util.List;

@Getter
@Setter
@ToString
public class Providers {
	private Conflicts conflicts;
	private List<ProviderEntry> providers;

	@Getter
	@Setter
	@ToString
	public static class ProviderEntry {
		private String id;
		private boolean enabled;
		private int priority;
	}

	@Getter
	@Setter
	@ToString
	public static class Conflicts {
		private List<String> keys;
		private String defaultPolicy;
		private List<ConflictPolicy> policies;
	}

	@Getter
	@Setter
	@ToString
	public static class ConflictPolicy {
		private List<String> when;
		private String key;
		private String priority;
		private Resolution resolution;
	}

	@Getter
	@Setter
	@ToString
	public static class Resolution {
		private Loser loser;
		private Winner winner;
		@Field(name = "new")
		private NewConnection newConnection;
	}

	@Getter
	@Setter
	@ToString
	public static class Loser {
		private String provider;
		private String solution;
		private String strategy;

		public String resolveSolutionId() {
			if (solution != null && !solution.isBlank())
				return solution;
			if (strategy != null && !strategy.isBlank())
				return strategy;

			return null;
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Winner {
		private String action;
	}

	@Getter
	@Setter
	@ToString
	public static class NewConnection {
		private String action;
		private String message;
		private Boolean prompt;
	}
}
