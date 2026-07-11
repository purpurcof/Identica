package me.whereareiam.identica.feature.verification.database.mapper;

import me.whereareiam.identica.feature.verification.database.entity.VerificationRecoveryCodeEntity;
import me.whereareiam.identica.feature.verification.model.VerificationRecoveryCode;

public final class VerificationRecoveryCodeMapper {
	public static VerificationRecoveryCode toModel(VerificationRecoveryCodeEntity entity) {
		return VerificationRecoveryCode.builder()
				.uniqueId(entity.getUniqueId())
				.methodId(entity.getMethodId())
				.codeHash(entity.getCodeHash())
				.createdAt(entity.getCreatedAt())
				.usedAt(entity.getUsedAt())
				.build();
	}

	public static VerificationRecoveryCodeEntity toEntity(VerificationRecoveryCode model) {
		return VerificationRecoveryCodeEntity.builder()
				.uniqueId(model.getUniqueId())
				.methodId(model.getMethodId())
				.codeHash(model.getCodeHash())
				.createdAt(model.getCreatedAt())
				.usedAt(model.getUsedAt())
				.build();
	}
}
