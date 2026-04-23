package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.Event;
import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import me.whereareiam.identica.type.identity.UniqueIdMode;
import me.whereareiam.identica.type.pipeline.PipelineConcurrencyPolicy;
import me.whereareiam.identica.type.pipeline.journey.JourneyPolicy;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.session.SessionConcurrencyPolicy;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Root settings configuration model.
 */
@Getter
@Setter
@ToString
public class Settings {
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
		private @NotNull Routing routing;
		private @NotNull Sessions sessions;
		private @NotNull AuthenticationScenario authentication;
		private @NotNull RegistrationScenario registration;
		private @NotNull MigrationScenario migration;
		private @NotNull Sentinels sentinels;

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
			private @NotNull RoutingAttemptPolicy attempts = RoutingAttemptPolicy.defaultStep();

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
			private @NotNull Target step = Target.step();
			/**
			 * Routing target used when a scenario journeyMode fully completes.
			 */
			private @NotNull Target complete = Target.complete();
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
		private @NotNull Duration defaultTtl;
		private @NotNull Duration refreshTtl;
		private @NotNull Map<String, Duration> providers = new HashMap<>();
		/**
		 * Default policy for concurrent sessions.
		 */
		private @NotNull SessionConcurrencyPolicy concurrencyPolicy;
		/**
		 * Policy overrides keyed by provider id.
		 */
		private @NotNull Map<String, SessionConcurrencyPolicy> concurrencyOverrides = new HashMap<>();
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
		/**
		 * Policy for concurrent sessions when a player is already online.
		 */
		private @NotNull SessionConcurrencyPolicy sessionConcurrencyPolicy;
		/**
		 * Policy for concurrent in-flight pipelines for the same identity.
		 */
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
		private @NotNull Map<String, Event> events;
	}
}
