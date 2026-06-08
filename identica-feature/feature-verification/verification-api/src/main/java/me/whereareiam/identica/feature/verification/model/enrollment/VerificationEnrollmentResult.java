package me.whereareiam.identica.feature.verification.model.enrollment;

import lombok.*;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessDisplay;
import me.whereareiam.identica.feature.verification.state.VerificationEnrollmentState;
import me.whereareiam.identica.feature.verification.type.status.VerificationEnrollmentStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Result payload for verification enrollment operations.
 *
 * @param <S> enrollment state type
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationEnrollmentResult<S extends VerificationEnrollmentState> {
	private VerificationEnrollmentStatus status;
	private String enrollmentId;
	private String methodId;
	private String providerId;
	private String autoSelectedProviderId;
	private String credential;
	private String label;
	private @Nullable S state;
	private @Nullable VerificationProcessDisplay display;
	private Map<String, String> methodData;
	private List<String> recoveryCodes;

	/**
	 * Creates an enrollment result.
	 *
	 * @param status enrollment status
	 * @param enrollmentId enrollment id
	 * @param methodId method id
	 * @param providerId provider id
	 * @param <S> enrollment state type
	 * @return enrollment result
	 */
	public static <S extends VerificationEnrollmentState> @NotNull VerificationEnrollmentResult<S> of(
			@NotNull VerificationEnrollmentStatus status,
			@Nullable String enrollmentId,
			@Nullable String methodId,
			@Nullable String providerId
	) {
		return VerificationEnrollmentResult.<S>builder()
				.status(status)
				.enrollmentId(enrollmentId)
				.methodId(methodId)
				.providerId(providerId)
				.build();
	}

	/**
	 * Creates a waiting enrollment result.
	 *
	 * @param enrollmentId enrollment id
	 * @param methodId method id
	 * @param providerId provider id
	 * @param state enrollment state
	 * @param display enrollment display payload
	 * @param <S> enrollment state type
	 * @return waiting enrollment result
	 */
	public static <S extends VerificationEnrollmentState> @NotNull VerificationEnrollmentResult<S> waiting(
			@NotNull String enrollmentId,
			@NotNull String methodId,
			@Nullable String providerId,
			@Nullable S state,
			@Nullable VerificationProcessDisplay display
	) {
		return VerificationEnrollmentResult.<S>builder()
				.status(VerificationEnrollmentStatus.WAITING)
				.enrollmentId(enrollmentId)
				.methodId(methodId)
				.providerId(providerId)
				.state(state)
				.display(display)
				.methodData(display != null ? display.getPlaceholders() : null)
				.build();
	}
}
