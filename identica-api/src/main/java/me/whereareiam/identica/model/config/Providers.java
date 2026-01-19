package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Getter
@Setter
@ToString
public class Providers {
	private Conflict conflicts;
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
	public static class Conflict {
		private List<String> keys;
		private List<ConflictPolicy> policies;
		private List<String> defaultActions;
	}

	@Getter
	@Setter
	@ToString
	public static class ConflictPolicy {
		private List<String> when;
		private String key;
		private List<String> actions;
	}
}
