package me.whereareiam.identica.model.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.UsernameSource;
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

	/**
	 * Source of the currently active username ("eligibility", "manual", "system").
	 */
	private @NotNull UsernameSource source;

	private long createdAt;
	private long lastSeenAt;
}
