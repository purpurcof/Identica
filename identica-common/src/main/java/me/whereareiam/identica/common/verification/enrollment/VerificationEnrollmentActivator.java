package me.whereareiam.identica.common.verification.enrollment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.verification.RecoveryCodeGenerator;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.verification.enroll.VerificationEnrollmentConfirmedEvent;
import me.whereareiam.identica.model.verification.VerificationRecoveryCode;
import me.whereareiam.identica.model.verification.enrollment.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationEnrollmentActivator {
	private final VerificationPersistenceService persistenceService;
	private final EventManager eventManager;

	public void activate(
			@NotNull UUID uniqueId,
			@NotNull PendingVerificationEnrollment record,
			@NotNull VerificationEnrollmentResult<?> result
	) {
		VerificationEnrollmentState state = result.getState();
		long now = System.currentTimeMillis();
		persistenceService.upsertEnrollment(VerificationEnrollment.builder()
				.uniqueId(uniqueId)
				.methodId(record.getMethodId())
				.enrollmentId(result.getEnrollmentId() != null && !result.getEnrollmentId().isBlank()
						? result.getEnrollmentId()
						: "default")
				.credential(state != null ? state.credential() : "")
				.label(state != null ? state.label() : "")
				.createdAt(record.getCreatedAt())
				.enabledAt(now)
				.build());

		List<VerificationRecoveryCode> codes = new ArrayList<>();
		if (result.getRecoveryCodes() != null) {
			for (String recoveryCode : result.getRecoveryCodes()) {
				codes.add(VerificationRecoveryCode.builder()
						.uniqueId(uniqueId)
						.methodId(record.getMethodId())
						.codeHash(RecoveryCodeGenerator.hash(recoveryCode))
						.createdAt(now)
						.usedAt(0L)
						.build());
			}
		}
		persistenceService.replaceRecoveryCodes(uniqueId, record.getMethodId(), codes);
		eventManager.call(new VerificationEnrollmentConfirmedEvent(uniqueId, record.getMethodId(), record.getProviderId()));
	}
}
