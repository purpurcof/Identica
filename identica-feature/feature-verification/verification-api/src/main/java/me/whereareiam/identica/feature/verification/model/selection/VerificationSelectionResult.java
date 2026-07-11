package me.whereareiam.identica.feature.verification.model.selection;

import lombok.*;
import me.whereareiam.identica.feature.verification.type.status.VerificationSelectionStatus;
import org.jetbrains.annotations.Nullable;

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

	/**
	 * Creates a selection result.
	 *
	 * @param status selection status
	 * @param methodId method id
	 * @param providerId provider id
	 * @return selection result
	 */
	public static VerificationSelectionResult of(
			VerificationSelectionStatus status,
			@Nullable String methodId,
			@Nullable String providerId
	) {
		return VerificationSelectionResult.builder()
				.status(status)
				.methodId(methodId)
				.providerId(providerId)
				.build();
	}
}
