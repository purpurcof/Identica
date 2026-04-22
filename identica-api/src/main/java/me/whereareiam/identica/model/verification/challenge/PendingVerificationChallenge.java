package me.whereareiam.identica.model.verification.challenge;

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
 * Pending challenge process record.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PendingVerificationChallenge {
	private @NotNull String challengeId;
	private @NotNull UUID uniqueId;
	private @NotNull String methodId;
	private @Nullable String providerId;
	private @Nullable String purpose;
	private boolean required;
	private @NotNull String stateType;
	private @NotNull String statePayload;
	private long createdAt;
	private long expiresAt;
}
