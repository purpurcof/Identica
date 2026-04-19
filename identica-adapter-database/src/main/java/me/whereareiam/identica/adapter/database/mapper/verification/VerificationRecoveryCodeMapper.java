package me.whereareiam.identica.adapter.database.mapper.verification;

import me.whereareiam.identica.adapter.database.entity.verification.VerificationRecoveryCodeEntity;
import me.whereareiam.identica.model.verification.VerificationRecoveryCode;

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
