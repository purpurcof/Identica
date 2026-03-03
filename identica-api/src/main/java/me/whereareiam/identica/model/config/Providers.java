package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.node.ObjectNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider configuration settings.
 */
@Getter
@Setter
@ToString
public class Providers {
	private @NotNull Map<String, ConflictRules> conflicts = new HashMap<>();
	private @NotNull List<ProviderEntry> providers = new ArrayList<>();

	/**
	 * Conflict rules for a single conflict key.
	 */
	@Getter
	@Setter
	@ToString
	public static class ConflictRules {
		@Field(name = "default")
		private @NotNull ConflictRule defaultRule;
		private @NotNull List<ConflictRule> pairs = new ArrayList<>();
	}

	/**
	 * Conflict rule for provider pairs or default scope.
	 */
	@Getter
	@Setter
	@ToString
	public static class ConflictRule {
		private @NotNull List<String> providers = new ArrayList<>();
		private boolean force;
		private @NotNull List<ResolverEntry> resolvers = new ArrayList<>();
	}

	/**
	 * Resolver entry for conflict rules.
	 */
	@Getter
	@Setter
	@ToString
	public static class ResolverEntry {
		private @NotNull String id;
		private @NotNull Node parameters = new ObjectNode();
	}

	/**
	 * Provider definition entry.
	 */
	@Getter
	@Setter
	@ToString
	public static class ProviderEntry {
		private @NotNull String id;
		private boolean enabled;
		private int priority;
		/**
		 * Hostnames (optionally with port) that map to this provider.
		 * Entries must use the format {@code host} or {@code host:port}.
		 */
		private @NotNull List<String> entrypoints = new ArrayList<>();
	}
}
