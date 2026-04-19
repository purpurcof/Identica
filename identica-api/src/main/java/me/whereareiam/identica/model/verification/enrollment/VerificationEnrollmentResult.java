package me.whereareiam.identica.model.verification.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.status.VerificationEnrollmentStatus;

import java.util.List;
import java.util.Map;

/**
 * Result payload for verification enrollment start and confirmation operations.
 *
 * <p>Depending on {@link #status}, optional fields may describe method-specific
 * enrollment bootstrap data, generated recovery codes, or auto-selection side
 * effects.</p>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationEnrollmentResult {
	private VerificationEnrollmentStatus status;
	private String methodId;
	private String providerId;
	private String autoSelectedProviderId;
	private Map<String, String> methodData;
	private List<String> recoveryCodes;
}
