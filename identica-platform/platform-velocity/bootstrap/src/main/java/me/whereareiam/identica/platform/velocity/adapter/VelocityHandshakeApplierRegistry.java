package me.whereareiam.identica.platform.velocity.adapter;

import com.google.inject.Singleton;
import me.whereareiam.identica.handshake.HandshakeApplier;
import me.whereareiam.identica.handshake.HandshakeApplierRegistry;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.platform.velocity.api.handshake.VelocityHandshakeContext;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public final class VelocityHandshakeApplierRegistry
		implements HandshakeApplierRegistry<VelocityHandshakeContext> {
	private final Set<HandshakeApplier<VelocityHandshakeContext>> appliers = new CopyOnWriteArraySet<>();

	@Override
	public void register(@NotNull HandshakeApplier<VelocityHandshakeContext> applier) {
		appliers.add(applier);
	}

	@Override
	public void unregister(@NotNull HandshakeApplier<VelocityHandshakeContext> applier) {
		appliers.remove(applier);
	}

	@Override
	public void applyAll(@NotNull VelocityHandshakeContext context, @NotNull HandshakeInstruction instruction) {
		for (HandshakeApplier<VelocityHandshakeContext> applier : appliers) {
			try {
				applier.apply(context, instruction);
			} catch (Exception e) {
				Logger.warn("Handshake instruction applier failed: %s", e.getMessage());
			}
		}
	}
}
