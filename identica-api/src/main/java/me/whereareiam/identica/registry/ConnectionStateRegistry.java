package me.whereareiam.identica.registry;

import me.whereareiam.identica.model.connection.ConnectionState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Registry for connection-scoped state such as routing and flow progress.
 */
@SuppressWarnings("unused")
public interface ConnectionStateRegistry {
	/**
	 * Returns an existing connection state or creates a new one.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return connection state
	 */
	@NotNull ConnectionState ensure(@NotNull UUID connectionUniqueId);

	/**
	 * Returns a connection state when present.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return optional connection state
	 */
	@NotNull Optional<ConnectionState> find(@NotNull UUID connectionUniqueId);

	/**
	 * Returns all tracked connection states.
	 *
	 * @return collection of connection states
	 */
	@NotNull Collection<ConnectionState> getStates();

	/**
	 * Clears the connection state for the provided id.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return {@code true} if a state was cleared
	 */
	boolean clear(@NotNull UUID connectionUniqueId);
}
