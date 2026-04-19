package me.whereareiam.identica.adapter.database.mapper.account;

import me.whereareiam.identica.adapter.database.entity.account.AccountProviderProfileEntity;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;

public final class AccountProviderProfileMapper {
	public static AccountProviderProfile toModel(AccountProviderProfileEntity entity) {
		if (entity == null) return null;
		return AccountProviderProfile.builder()
				.providerId(entity.getProviderId())
				.providerSubject(entity.getProviderSubject())
				.providerUsername(entity.getProviderUsername())
				.build();
	}

	public static AccountProviderProfileEntity toEntity(AccountProviderProfile profile) {
		if (profile == null) return null;
		return AccountProviderProfileEntity.builder()
				.providerId(profile.getProviderId())
				.providerSubject(profile.getProviderSubject())
				.providerUsername(profile.getProviderUsername())
				.build();
	}
}
