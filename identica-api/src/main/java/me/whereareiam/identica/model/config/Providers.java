package me.whereareiam.identica.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.merge.strategy.type.StructuralObject;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import me.whereareiam.identica.type.session.recognition.RecognitionSignal;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class Providers extends ConfigDocument {
	private @NotNull Map<String, ConflictRules> conflicts = new HashMap<>();
	private @NotNull List<ProviderEntry> providers = new ArrayList<>();

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

	/**
	 * Provider definition entry.
	 */
	@Getter
	@Setter
	@ToString
	public static class ProviderEntry {
		private @NotNull String id;
		/**
		 * Optional user-facing label for this provider.
		 * Falls back to the provider descriptor name and then the raw id.
		 */
		private @NotNull String displayName = "";
		private boolean enabled;
		private int priority;
		/**
		 * Optional provider-specific overrides.
		 */
		@Merge(StructuralObject.class)
		private @NotNull Overrides overrides = new Overrides();
		private @NotNull JoinRestriction joinRestriction = new JoinRestriction();
		private @NotNull Verification verification = new Verification();
		/**
		 * Hostnames (optionally with port) that map to this provider.
		 * Entries must use the format {@code host} or {@code host:port}.
		 */
		private @NotNull List<String> entrypoints = new ArrayList<>();

		@Getter
		@Setter
		@ToString
		public static class Overrides {
			private @Nullable SessionConcurrencyPolicy sessionConcurrencyPolicy;
			private @NotNull Recognition recognition = new Recognition();

			@Getter
			@Setter
			@ToString
			public static class Recognition {
				private @Nullable Boolean enabled;
				private @NotNull List<RecognitionSignal> signals = new ArrayList<>();
				private @NotNull Eligibility eligibility = new Eligibility();

				@Getter
				@Setter
				@ToString
				public static class Eligibility {
					private boolean allowOnUntrustedIp;
				}
			}
		}

		@Getter
		@Setter
		@ToString
		public static class JoinRestriction {
			private boolean enabled;
			private @Nullable List<ProviderJoinRestrictionCondition> allow;
		}

		@Getter
		@Setter
		@ToString
		public static class Verification {
			private boolean enabled;
			private boolean required;
			private @Nullable UnavailableSelectionPolicy unavailableSelectionPolicy;
			private @NotNull List<MethodEntry> methods = new ArrayList<>();

			@Getter
			@Setter
			@ToString
			public static class MethodEntry {
				private @NotNull String id = "";
				private boolean enabled;
				private int priority;
				private @Nullable Boolean required;
				private @Nullable UnavailableSelectionPolicy unavailableSelectionPolicy;
			}
		}
	}
}
