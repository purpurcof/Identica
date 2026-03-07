package me.whereareiam.identica.model.auth.handshake;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Request details for the handshake phase.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class HandshakeRequest {
	private final @NotNull ConnectionIdentity identity;
	private final @Nullable ProviderContext provider;
}
