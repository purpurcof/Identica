package me.whereareiam.identica.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a persisted username change for an account.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UsernameHistoryEntry {
	private @NotNull UUID uniqueId;
	private @Nullable String providerId;
	private @NotNull String oldUsername;
	private @NotNull String newUsername;
	private @NotNull String source;
	private long changedAt;
}
