package me.whereareiam.identica.adapter.database.mapper.verification;

import me.whereareiam.identica.adapter.database.entity.verification.VerificationSelectionEntity;
import me.whereareiam.identica.model.verification.VerificationSelection;

public final class VerificationSelectionMapper {
	public static VerificationSelection toModel(VerificationSelectionEntity entity) {
		return VerificationSelection.builder()
				.uniqueId(entity.getUniqueId())
				.providerId(entity.getProviderId())
				.methodId(entity.getMethodId())
				.selectedAt(entity.getSelectedAt())
				.build();
	}

	public static VerificationSelectionEntity toEntity(VerificationSelection model) {
		return VerificationSelectionEntity.builder()
				.uniqueId(model.getUniqueId())
				.providerId(model.getProviderId())
				.methodId(model.getMethodId())
				.selectedAt(model.getSelectedAt())
				.build();
	}
}
