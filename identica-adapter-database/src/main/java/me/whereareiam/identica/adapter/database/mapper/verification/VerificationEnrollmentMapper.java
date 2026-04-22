package me.whereareiam.identica.adapter.database.mapper.verification;

import me.whereareiam.identica.adapter.database.entity.verification.VerificationEnrollmentEntity;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;

public final class VerificationEnrollmentMapper {
	public static VerificationEnrollment toModel(VerificationEnrollmentEntity entity) {
		return VerificationEnrollment.builder()
				.uniqueId(entity.getUniqueId())
				.methodId(entity.getMethodId())
				.enrollmentId(entity.getEnrollmentId())
				.credential(entity.getCredential())
				.label(entity.getLabel())
				.createdAt(entity.getCreatedAt())
				.enabledAt(entity.getEnabledAt())
				.build();
	}

	public static VerificationEnrollmentEntity toEntity(VerificationEnrollment model) {
		return VerificationEnrollmentEntity.builder()
				.uniqueId(model.getUniqueId())
				.methodId(model.getMethodId())
				.enrollmentId(model.getEnrollmentId())
				.credential(model.getCredential())
				.label(model.getLabel())
				.createdAt(model.getCreatedAt())
				.enabledAt(model.getEnabledAt())
				.build();
	}
}
