package me.whereareiam.identica.model.identity;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Internal Identica account representing a single player within the system.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Account {
	/**
	 * Internal Identica UUID (not Mojang/offline).
	 */
	private @NotNull UUID uniqueId;
	/**
	 * Identica-controlled username visible to the server.
	 */
	private @NotNull String username;

	private long createdAt;
	private long lastSeenAt;
}
