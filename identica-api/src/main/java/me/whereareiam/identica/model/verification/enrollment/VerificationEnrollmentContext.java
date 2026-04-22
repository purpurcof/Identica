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
