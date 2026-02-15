package me.whereareiam.identica.provider.cracked.database.mapper;

import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.database.entity.CrackedAccountEntity;

public final class CrackedAccountMapper {
	public static CrackedAccount toModel(CrackedAccountEntity entity) {
		if (entity == null) return null;
		return CrackedAccount.builder()
				.providerId(entity.getProviderId())
				.providerSubject(entity.getProviderSubject())
				.passwordHash(entity.getPasswordHash())
				.hashingMethod(entity.getHashingMethod())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}

	public static CrackedAccountEntity toEntity(CrackedAccount account) {
		if (account == null) return null;
		return CrackedAccountEntity.builder()
				.providerId(account.getProviderId())
				.providerSubject(account.getProviderSubject())
				.passwordHash(account.getPasswordHash())
				.hashingMethod(account.getHashingMethod())
				.createdAt(account.getCreatedAt())
				.updatedAt(account.getUpdatedAt())
				.build();
	}
}
