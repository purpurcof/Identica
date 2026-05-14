package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.merge.strategy.type.DeclaredKeysOnlyMap;
import me.whereareiam.configura.merge.strategy.type.DefaultKeysOnlyMap;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import me.whereareiam.identica.type.identity.UniqueIdMode;
import me.whereareiam.identica.type.pipeline.PipelineConcurrencyPolicy;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.JourneyPolicy;
import me.whereareiam.identica.type.session.RecognitionSignal;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class Settings extends ConfigDocument {
	/**
	 * Verbosity level for logging.
	 */
	private int level;
	private @NotNull Connection connection;
	private @NotNull Listeners listeners;

	@Getter
	@Setter
	@ToString
	public static class Connection {
		/**
		 * Time-to-live for handshake instructions.
		 */
		private @NotNull Duration handshakeInstructionTtl;
		/**
		 * Time-to-live for provider attempt markers.
		 */
		private @NotNull Duration attemptTtl;
		/**
		 * Time-to-live for reserved account identities.
		 */
		private @NotNull Duration reservationTtl;
		/**
		 * Time-to-live for transient prepare-state bridge entries.
		 */
		private @NotNull Duration prepareStateTtl;
		/**
		 * Strategy used to assign UUIDs to newly discovered accounts.
		 */
		private @NotNull UniqueIdMode uniqueIdMode;
		private @NotNull Routing routing = new Routing();
		private @NotNull Sessions sessions = new Sessions();
		private @NotNull InitialPrompt initialPrompt = new InitialPrompt();
		private @NotNull Scenarios scenarios = new Scenarios();
		private @NotNull Sentinels sentinels = new Sentinels();

		/**
		 * Returns handshake instruction TTL in milliseconds with validation.
		 *
		 * @return handshake instruction TTL in milliseconds
		 */
		public long handshakeInstructionTtlMillis() {
			if (handshakeInstructionTtl.isZero() || handshakeInstructionTtl.isNegative()) {
				throw new IllegalStateException("settings.connection.handshakeInstructionTtl must be positive");
			}

			return handshakeInstructionTtl.toMillis();
		}

		/**
		 * Returns provider attempt TTL in milliseconds with validation.
		 *
		 * @return provider attempt TTL in milliseconds
		 */
		public long attemptTtlMillis() {
			if (attemptTtl.isZero() || attemptTtl.isNegative()) {
				throw new IllegalStateException("settings.connection.attemptTtl must be positive");
			}

			return attemptTtl.toMillis();
		}

		/**
		 * Returns prepare-state TTL in milliseconds with validation.
		 *
		 * @return prepare-state TTL in milliseconds
		 */
		public long prepareStateTtlMillis() {
			if (prepareStateTtl.isZero() || prepareStateTtl.isNegative()) {
				throw new IllegalStateException("settings.connection.prepareStateTtl must be positive");
			}

			return prepareStateTtl.toMillis();
		}

	}

	@Getter
	@Setter
	@ToString
	public static class InitialPrompt {
		private boolean resendUntilInteraction;
		private @NotNull Duration resendInterval;

		public long resendIntervalMillis() {
			if (resendInterval.isZero() || resendInterval.isNegative())
				throw new IllegalStateException("settings.connection.initialPrompt.resendInterval must be positive");

			return resendInterval.toMillis();
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Sentinels {
		/**
		 * Rate limit applied when clients spam pipeline resume/advance requests.
		 */
		private @NotNull SentinelPolicy resumeSpam;
	}

	@Getter
	@Setter
	@ToString
	public static class Routing {
		private @NotNull Defaults defaults = new Defaults();
		/**
		 * Scenario-specific routing targets keyed by scenario id.
		 * Supported ids: authentication, registration, migration.
		 */
		@Merge(DeclaredKeysOnlyMap.class)
		private @NotNull Map<String, Targets> scenarios = new HashMap<>();

		@Getter
		@Setter
		@ToString
		public static class Defaults {
			private @NotNull Target step = Target.step();
			private @NotNull Target complete = Target.complete();
		}

		@Getter
		@Setter
		@ToString
		public static class Target {
			private @NotNull String target = "";
			private @Nullable RoutingAttemptPolicy attempts;

			public static @NotNull Target step() {
				Target target = new Target();
				target.setAttempts(RoutingAttemptPolicy.defaultStep());
				return target;
			}

			public static @NotNull Target complete() {
				Target target = new Target();
				target.setAttempts(RoutingAttemptPolicy.defaultCompletion());
				return target;
			}
		}

		/**
		 * Routing targets by phase.
		 */
		@Getter
		@Setter
		@ToString
		public static class Targets {
			private @NotNull Target step = new Target();
			/**
			 * Routing target used when a scenario journeyMode fully completes.
			 */
			private @NotNull Target complete = new Target();
			private @NotNull Overrides overrides = new Overrides();

			/**
			 * Routing overrides.
			 */
			@Getter
			@Setter
			@ToString
			public static class Overrides {
				private @NotNull Map<String, Target> stages = new HashMap<>();
				private @NotNull Map<String, Target> steps = new HashMap<>();
			}
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Sessions {
		/**
		 * Default policy for concurrent sessions.
		 */
		private @NotNull SessionConcurrencyPolicy concurrencyPolicy;
		/**
		 * Time-to-live used to keep live-session cache entries available while the player is online.
		 */
		private @NotNull Duration activeTtl;
		/**
		 * Recognition policy for reconnecting players.
		 */
		private @NotNull Recognition recognition = new Recognition();

		public long activeTtlMillis() {
			if (activeTtl.isZero() || activeTtl.isNegative())
				throw new IllegalStateException("settings.connection.sessions.activeTtl must be positive");

			return activeTtl.toMillis();
		}

		@Getter
		@Setter
		@ToString
		public static class Recognition {
			/**
			 * Enables reconnect recognition for eligible providers.
			 */
			private boolean enabled;
			/**
			 * Amount of time a stored recognition snapshot remains valid.
			 */
			private @NotNull Duration validity;
			/**
			 * Default signal set used when providers do not override reconnect recognition signals.
			 */
			private @NotNull List<RecognitionSignal> defaultSignals = new ArrayList<>();
			/**
			 * Guard configuration that suppresses automatic reconnect recognition from configured client IP ranges.
			 */
			private @NotNull UntrustedIps untrustedIps = new UntrustedIps();

			public long validityMillis() {
				if (validity.isZero() || validity.isNegative())
					throw new IllegalStateException("settings.connection.sessions.recognition.validity must be positive");

				return validity.toMillis();
			}

			/**
			 * Guard configuration for automatic reconnect recognition coming from untrusted client IPs.
			 */
			@Getter
			@Setter
			@ToString
			public static class UntrustedIps {
				/**
				 * Enables the untrusted-IP suppression guard.
				 */
				private boolean enabled;
				/**
				 * Exact IPs or CIDR ranges that should suppress automatic reconnect recognition.
				 */
				private @NotNull List<String> entries = new ArrayList<>();
			}
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Scenarios {
		private @NotNull AuthenticationScenario authentication = new AuthenticationScenario();
		private @NotNull RegistrationScenario registration = new RegistrationScenario();
		private @NotNull MigrationScenario migration = new MigrationScenario();
	}

	@Getter
	@Setter
	@ToString
	public static class Scenario {
		/**
		 * Time-to-live for pending pipeline state.
		 */
		private @NotNull Duration pipelineTtl;
		/**
		 * Time-to-live for advance locks.
		 */
		private @NotNull Duration advanceLockTtl;
		/**
		 * Whether resume requests are allowed for this scenario.
		 */
		private boolean allowResume;
		/**
		 * Preferred journey mode for this scenario.
		 */
		private @NotNull JourneyMode journeyMode;
		/**
		 * Policy for applying the preferred journey mode.
		 */
		private @NotNull JourneyPolicy journeyPolicy;

		/**
		 * Returns pipeline TTL in milliseconds with validation.
		 *
		 * @return pipeline TTL in milliseconds
		 */
		public long pipelineTtlMillis() {
			if (pipelineTtl.isZero() || pipelineTtl.isNegative()) {
				throw new IllegalStateException("settings.connection.pipelineTtl must be positive");
			}

			return pipelineTtl.toMillis();
		}

		/**
		 * Returns advance lock TTL in milliseconds with validation.
		 *
		 * @return advance lock TTL in milliseconds
		 */
		public long advanceLockTtlMillis() {
			if (advanceLockTtl.isZero() || advanceLockTtl.isNegative()) {
				throw new IllegalStateException("settings.connection.advanceLockTtl must be positive");
			}

			return advanceLockTtl.toMillis();
		}
	}

	@Getter
	@Setter
	@ToString
	public static class AuthenticationScenario extends Scenario {
		private @NotNull PipelineConcurrencyPolicy pipelineConcurrencyPolicy;
	}

	@Getter
	@Setter
	@ToString
	public static class RegistrationScenario extends Scenario {
		/**
		 * Policy for concurrent in-flight pipelines for the same identity.
		 */
		private @NotNull PipelineConcurrencyPolicy pipelineConcurrencyPolicy;
		/**
		 * Whether interactive registration should auto-select the only available provider.
		 */
		private boolean autoSelectSingleProvider;
	}

	@Getter
	@Setter
	@ToString
	public static class MigrationScenario extends Scenario {
	}

	@Getter
	@Setter
	@ToString
	public static class Listeners {
		@Merge(DefaultKeysOnlyMap.class)
		private @NotNull Map<String, Event> events;
	}
}
