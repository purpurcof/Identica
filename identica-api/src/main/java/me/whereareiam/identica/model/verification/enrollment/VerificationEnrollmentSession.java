package me.whereareiam.identica.model.verification.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.VerificationPendingStage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationEnrollmentSession {
	private UUID uniqueId;
	private String username;
	private String providerId;
	private String methodId;
	private VerificationPendingStage stage;
	private String payload;
	private Map<String, String> methodData;
	private List<String> recoveryCodes;
	private long createdAt;
}
