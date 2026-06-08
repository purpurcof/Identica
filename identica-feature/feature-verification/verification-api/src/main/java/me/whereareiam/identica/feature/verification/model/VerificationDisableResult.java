package me.whereareiam.identica.feature.verification.model;

import lombok.*;
import me.whereareiam.identica.feature.verification.type.status.VerificationDisableStatus;

/**
 * Result payload for verification method disable operations.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationDisableResult {
	private VerificationDisableStatus status;
	private String methodId;
}
