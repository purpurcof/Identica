package me.whereareiam.identica.service;

import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Platform-specific adapter for emitting messaging readiness checkpoints.
 *
 * <p>Each platform must bind exactly one implementation.</p>
 */
public interface PlatformDeliveryAdapter {
	/**
	 * Returns the messaging checkpoint used for deferred initial prompt messaging on this platform.
	 *
	 * @return logical checkpoint for waiting prompt messaging
	 */
	@NotNull DeliveryCheckpoint initialPromptCheckpoint();

	/**
	 * Arms messaging for the initial platform-ready checkpoint.
	 *
	 * @param identity online identity associated with the connection
	 * @param currentServer current target server name when known
	 */
	void armInitialReady(@NotNull Identity identity, @Nullable String currentServer);

	/**
	 * Clears any in-flight readiness tracking for the given connection.
	 *
	 * @param connectionUniqueId live connection UUID
	 */
	void clear(@NotNull UUID connectionUniqueId);
}
