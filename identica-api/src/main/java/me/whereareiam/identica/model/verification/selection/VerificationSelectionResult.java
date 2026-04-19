package me.whereareiam.identica.model.verification.selection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.status.VerificationSelectionStatus;

/**
 * Result payload for provider verification-method selection changes.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationSelectionResult {
	private VerificationSelectionStatus status;
	private String methodId;
	private String providerId;
}
