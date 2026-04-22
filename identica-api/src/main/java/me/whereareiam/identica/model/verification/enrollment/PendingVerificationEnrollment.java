package me.whereareiam.identica.model.verification.enrollment;

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
 * Pending enrollment process record.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PendingVerificationEnrollment {
	private @NotNull String enrollmentId;
	private @NotNull UUID uniqueId;
	private @NotNull String username;
	private @NotNull String methodId;
	private @Nullable String providerId;
	private @NotNull String stateType;
	private @NotNull String statePayload;
	private long createdAt;
	private long expiresAt;
}
