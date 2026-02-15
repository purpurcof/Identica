package me.whereareiam.identica.provider.cracked.database.mapper;

import me.whereareiam.identica.provider.cracked.model.CrackedAccountPassword;
import me.whereareiam.identica.provider.cracked.database.entity.CrackedAccountPasswordEntity;

public final class CrackedAccountPasswordMapper {
	public static CrackedAccountPasswordEntity toEntity(CrackedAccountPassword change) {
		if (change == null) return null;
		return CrackedAccountPasswordEntity.builder()
				.providerId(change.getProviderId())
				.providerSubject(change.getProviderSubject())
				.hashingMethod(change.getHashingMethod())
				.changeReason(change.getChangeReason().name())
				.changedAt(change.getChangedAt())
				.build();
	}
}
