package me.whereareiam.identica.feature.verification.model.enrollment;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationEnrollment {
	private UUID uniqueId;
	private String methodId;
	private String enrollmentId;
	private String credential;
	private String label;
	private long createdAt;
	private long enabledAt;
}
