package me.whereareiam.identica.handshake;

import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Central store for handshake policies and instructions.
 */
public interface HandshakeStore {
	void registerPolicy(@NotNull HandshakePolicy policy);

	void unregisterPolicy(@NotNull HandshakePolicy policy);

	@NotNull Set<HandshakePolicy> policies();

	void putInstruction(@NotNull HandshakeInstruction instruction);

	@NotNull Optional<HandshakeInstruction> consumeInstruction(@NotNull String username, @NotNull String ip);

	void invalidateInstruction(@NotNull String username, @NotNull String ip);
}
