package me.whereareiam.identica.provider.credential.database.mapper;

import me.whereareiam.identica.provider.credential.database.entity.CredentialAccountHistoryEntity;
import me.whereareiam.identica.provider.credential.model.CredentialAccountHistory;

public final class CredentialAccountHistoryMapper {
	public static CredentialAccountHistoryEntity toEntity(CredentialAccountHistory history) {
		if (history == null) return null;
		return CredentialAccountHistoryEntity.builder()
				.providerId(history.getProviderId())
				.providerSubject(history.getProviderSubject())
				.hashingMethod(history.getHashingMethod())
				.changeReason(history.getChangeReason().name())
				.changedAt(history.getChangedAt())
				.build();
	}
}
