package me.whereareiam.identica.feature.verification.model.enrollment;

import lombok.*;
import me.whereareiam.identica.feature.verification.state.VerificationEnrollmentState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Context supplied to a method enrollment process.
 *
 * @param <S> enrollment state type
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationEnrollmentContext<S extends VerificationEnrollmentState> {
	private @NotNull String enrollmentId;
	private @NotNull UUID subjectUniqueId;
	private @NotNull String username;
	private @NotNull String methodId;
	private @Nullable String providerId;
	private @Nullable S state;
}
