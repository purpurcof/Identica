package me.whereareiam.identica.model.verification.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
	private String payload;
	private long createdAt;
	private long enabledAt;
}
