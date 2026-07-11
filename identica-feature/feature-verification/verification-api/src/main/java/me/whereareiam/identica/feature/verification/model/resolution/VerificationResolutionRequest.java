package me.whereareiam.identica.feature.verification.model.resolution;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Request used to resolve the provider verification requirement.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationResolutionRequest {
	private @NotNull UUID uniqueId;
	private @NotNull String providerId;
	private @Nullable String purpose;
}
