package me.whereareiam.identica.feature.verification.model;

import lombok.*;
import me.whereareiam.identica.feature.verification.type.status.VerificationResetStatus;

/**
 * Result payload for verification reset operations.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationResetResult {
	private VerificationResetStatus status;
	private String providerId;
}
