package me.whereareiam.identica.model.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Request details for the handshake phase.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class HandshakeRequest {
	private final String username;
}
