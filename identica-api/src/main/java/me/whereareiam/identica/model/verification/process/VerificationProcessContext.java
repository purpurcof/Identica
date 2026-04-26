package me.whereareiam.identica.model.verification.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared context supplied to a verification process step.
 *
 * @param <S> state type
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationProcessContext<S extends VerificationProcessState> {
	private @NotNull UUID subjectUniqueId;
	private @NotNull String methodId;
	private @Nullable String providerId;
	private @Nullable String purpose;
	private @Nullable String processId;
	private @Nullable String username;
	@Builder.Default
	private @NotNull List<VerificationEnrollment> enrollments = new ArrayList<>();
	private @Nullable S state;
}
