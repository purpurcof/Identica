package me.whereareiam.identica.feature.verification.model.enrollment;

import lombok.*;
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
