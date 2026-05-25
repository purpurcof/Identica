package me.whereareiam.identica.model.config.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conflict resolution configuration settings.
 */
@Getter
@Setter
@ToString
public class Conflicts extends ConfigDocument {
	private @NotNull Map<String, ConflictRules> rules = new HashMap<>();

	/**
	 * Conflict rules for a single conflict key.
	 */
	@Getter
	@Setter
	@ToString
	public static class ConflictRules {
		@JsonProperty("default")
		private @NotNull ConflictRule defaultRule;
		private @NotNull List<ConflictRule> pairs = new ArrayList<>();

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

			/**
			 * Resolver entry for conflict rules.
			 */
			@Getter
			@Setter
			@ToString
			public static class ResolverEntry {
				private @NotNull String id;
				private @NotNull JsonNode parameters = JsonNodeFactory.instance.objectNode();
			}
		}
	}
}
