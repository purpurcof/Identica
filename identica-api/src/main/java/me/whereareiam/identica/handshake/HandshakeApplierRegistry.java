package me.whereareiam.identica.handshake;

import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import org.jetbrains.annotations.NotNull;

/**
 * Registry for platform-specific handshake instruction appliers.
 */
public interface HandshakeApplierRegistry<C extends HandshakeContext> {
	void register(@NotNull HandshakeApplier<C> applier);

	void unregister(@NotNull HandshakeApplier<C> applier);

	void applyAll(@NotNull C context, @NotNull HandshakeInstruction instruction);
}
