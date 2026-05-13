package me.whereareiam.identica.provider.credential.database.mapper;

import me.whereareiam.identica.provider.credential.database.entity.CredentialAccountEntity;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;

public final class CredentialAccountMapper {
	public static CredentialAccount toModel(CredentialAccountEntity entity) {
		if (entity == null) return null;
		return CredentialAccount.builder()
				.providerId(entity.getProviderId())
				.providerSubject(entity.getProviderSubject())
				.passwordHash(entity.getPasswordHash())
				.hashingMethod(entity.getHashingMethod())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}

	public static CredentialAccountEntity toEntity(CredentialAccount credential) {
		if (credential == null) return null;
		return CredentialAccountEntity.builder()
				.providerId(credential.getProviderId())
				.providerSubject(credential.getProviderSubject())
				.passwordHash(credential.getPasswordHash())
				.hashingMethod(credential.getHashingMethod())
				.createdAt(credential.getCreatedAt())
				.updatedAt(credential.getUpdatedAt())
				.build();
	}
}
