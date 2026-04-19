package me.whereareiam.identica.model.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.status.VerificationTargetType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationTarget {
	private UUID subjectUniqueId;
	private VerificationTargetType type;
	private String key;
	private String providerId;
	private String purpose;

	public static @NotNull VerificationTarget providerSelection(
			@NotNull UUID subjectUniqueId,
			@NotNull String providerId,
			@Nullable String purpose
	) {
		return VerificationTarget.builder()
				.subjectUniqueId(subjectUniqueId)
				.type(VerificationTargetType.PROVIDER_SELECTION)
				.key(providerId)
				.providerId(providerId)
				.purpose(purpose)
				.build();
	}

	public static @NotNull VerificationTarget methodEnrollment(
			@NotNull UUID subjectUniqueId,
			@NotNull String methodId,
			@Nullable String providerId,
			@Nullable String purpose
	) {
		return VerificationTarget.builder()
				.subjectUniqueId(subjectUniqueId)
				.type(VerificationTargetType.METHOD_ENROLLMENT)
				.key(methodId)
				.providerId(providerId)
				.purpose(purpose)
				.build();
	}
}
