package me.whereareiam.identica.handshake;

import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import org.jetbrains.annotations.NotNull;

/**
 * Applies handshake instructions to a platform-specific context.
 */
public interface HandshakeApplier<C extends HandshakeContext> {
	void apply(@NotNull C context, @NotNull HandshakeInstruction instruction);
}
