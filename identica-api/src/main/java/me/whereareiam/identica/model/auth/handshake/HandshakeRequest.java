package me.whereareiam.identica.model.auth.handshake;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import org.jetbrains.annotations.NotNull;

/**
 * Request details for the handshake phase.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class HandshakeRequest {
	private final @NotNull OfflineIdentity identity;
}
