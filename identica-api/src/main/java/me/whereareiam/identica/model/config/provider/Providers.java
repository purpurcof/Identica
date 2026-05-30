package me.whereareiam.identica.model.config.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.configura.annotation.PreserveUnknownFields;
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.annotation.merge.MergeList;
import me.whereareiam.configura.feature.extension.api.annotation.ExtendableDocument;
import me.whereareiam.configura.merge.strategy.DeclaredObjectDefaults;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListPresence;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import me.whereareiam.identica.type.session.recognition.RecognitionSignal;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider configuration settings.
 */
@Getter
@Setter
@ToString
public class Providers extends ConfigDocument {
	private @NotNull Behavior behavior = new Behavior();
	@Merge
	@MergeList(
			mode = ListMode.KEYED,
			key = "id",
			presence = ListPresence.DECLARED_ONLY,
			unknownEntries = ListUnknownEntries.ALLOW
	)
	private @NotNull List<ProviderEntry> providers = new ArrayList<>();

	/**
	 * Shared provider runtime behavior settings.
	 */
	@Getter
	@Setter
	@ToString
	public static class Behavior {
		/**
		 * Time-to-live for provider attempt markers.
		 */
		private @NotNull Duration attemptTtl;
		/**
		 * Time-to-live for provider join restriction runtime toggles.
		 */
		private @NotNull Duration joinRestrictionToggleTtl;

		/**
		 * Returns attempt TTL in milliseconds with validation.
		 *
		 * @return attempt TTL in milliseconds
		 */
		public long attemptTtlMillis() {
			if (attemptTtl.isZero() || attemptTtl.isNegative()) {
				throw new IllegalStateException("providers.behavior.attemptTtl must be positive");
			}

			return attemptTtl.toMillis();
		}

		/**
		 * Returns join restriction toggle TTL in milliseconds with validation.
		 *
		 * @return join restriction toggle TTL in milliseconds
		 */
		public long joinRestrictionToggleTtlMillis() {
			if (joinRestrictionToggleTtl.isZero() || joinRestrictionToggleTtl.isNegative()) {
				throw new IllegalStateException("providers.behavior.joinRestrictionToggleTtl must be positive");
			}

			return joinRestrictionToggleTtl.toMillis();
		}
	}

	/**
	 * Provider definition entry.
	 */
	@Getter
	@Setter
	@ToString
	@ExtendableDocument
	@PreserveUnknownFields
	public static class ProviderEntry {
		private @NotNull String id;
		/**
		 * Optional user-facing label for this provider.
		 * Falls back to the provider descriptor name and then the raw id.
		 */
		private @Nullable String displayName;
		private boolean enabled;
		private int priority;
		/**
		 * Provider-specific session settings.
		 */
		@Merge(DeclaredObjectDefaults.class)
		@ExtendableDocument
		private @Nullable Session session;
		/**
		 * Provider-specific restriction settings.
		 */
		@Merge(DeclaredObjectDefaults.class)
		@ExtendableDocument
		private @Nullable Restriction restriction;
		@Merge(DeclaredObjectDefaults.class)
		@ExtendableDocument
		private @Nullable Verification verification;
		/**
		 * Hostnames (optionally with port) that map to this provider.
		 * Entries must use the format {@code host} or {@code host:port}.
		 */
		private @NotNull List<String> entrypoints = new ArrayList<>();

		/**
		 * Provider-specific session behavior settings.
		 */
		@Getter
		@Setter
		@ToString
		@ExtendableDocument
		public static class Session {
			private @Nullable SessionConcurrencyPolicy concurrencyPolicy;
			@Merge(DeclaredObjectDefaults.class)
			@ExtendableDocument
			private @Nullable Recognition recognition;

			/**
			 * Provider-specific reconnect recognition settings.
			 */
			@Getter
			@Setter
			@ToString
			@ExtendableDocument
			public static class Recognition {
				private @Nullable Boolean enabled;
				private @NotNull List<RecognitionSignal> signals = new ArrayList<>();
				private boolean allowOnUntrustedIps;
			}
		}

		/**
		 * Provider-specific restriction settings.
		 */
		@Getter
		@Setter
		@ToString
		@ExtendableDocument
		public static class Restriction {
			/**
			 * Runtime-toggleable provider join restriction settings.
			 */
			@Merge(DeclaredObjectDefaults.class)
			@ExtendableDocument
			private @Nullable Join join;

			/**
			 * Runtime-toggleable provider join restriction settings.
			 */
			@Getter
			@Setter
			@ToString
			@ExtendableDocument
			public static class Join {
				private boolean enabled;
				private @Nullable List<ProviderJoinRestrictionCondition> allow;
			}
		}

		/**
		 * Shared verification settings for a provider.
		 */
		@Getter
		@Setter
		@ToString
		@ExtendableDocument
		public static class Verification {
			private boolean enabled;
			private boolean required;
			private @Nullable UnavailableSelectionPolicy unavailableSelectionPolicy;
			@Merge
			@MergeList(
					mode = ListMode.KEYED,
					key = "id",
					presence = ListPresence.DECLARED_ONLY,
					unknownEntries = ListUnknownEntries.ALLOW
			)
			private @NotNull List<MethodEntry> methods = new ArrayList<>();

			/**
			 * Verification method entry for a provider.
			 */
			@Getter
			@Setter
			@ToString
			@ExtendableDocument
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
