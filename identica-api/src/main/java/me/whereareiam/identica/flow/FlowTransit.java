package me.whereareiam.identica.flow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * Short-lived transit storage for passing typed signals across authentication flow phases.
 */
@SuppressWarnings("unused")
public interface FlowTransit {
	/**
	 * Creates a scoped fluent view for the provided flow reference.
	 *
	 * @param ref flow reference
	 * @return scoped flow view
	 */
	@NotNull Scope scope(@NotNull FlowRefeference ref);

	/**
	 * Scoped fluent operations for a specific flow reference.
	 */
	interface Scope {
		/**
		 * Stores a signal value and returns a TTL builder.
		 *
		 * @param signal signal descriptor
		 * @param value signal value
		 * @param <T> signal type
		 * @return TTL builder
		 */
		@NotNull <T> Put<T> put(@NotNull FlowSignal<T> signal, @NotNull T value);

		/**
		 * Reads a signal value without invalidating it.
		 *
		 * @param signal signal descriptor
		 * @param <T> signal type
		 * @return optional signal value
		 */
		@NotNull <T> Optional<T> peek(@NotNull FlowSignal<T> signal);

		/**
		 * Reads and invalidates a signal value.
		 *
		 * @param signal signal descriptor
		 * @param <T> signal type
		 * @return optional signal value
		 */
		@NotNull <T> Optional<T> consume(@NotNull FlowSignal<T> signal);

		/**
		 * Invalidates all known signal values for this scope.
		 */
		void clear();
	}

	/**
	 * Fluent TTL writer for put operations.
	 *
	 * @param <T> signal type
	 */
	interface Put<T> {
		/**
		 * Stores the value with TTL.
		 *
		 * @param ttl signal TTL
		 */
		void ttl(@Nullable Duration ttl);

		/**
		 * Stores the value with TTL in milliseconds.
		 *
		 * @param ttlMs signal TTL in milliseconds
		 */
		default void ttlMillis(long ttlMs) {
			if (ttlMs <= 0)
				return;

			ttl(Duration.ofMillis(ttlMs));
		}
	}
}
