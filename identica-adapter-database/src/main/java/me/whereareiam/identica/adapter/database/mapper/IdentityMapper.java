package me.whereareiam.identica.adapter.database.mapper;

import me.whereareiam.identica.adapter.database.entity.IdentityEntity;
import me.whereareiam.identica.model.Identity;

public final class IdentityMapper {
	public static Identity toModel(IdentityEntity entity) {
		if (entity == null) return null;

		return Identity.builder()
				.uniqueId(entity.getUniqueId())
				.providerId(entity.getProviderId())
				.providerSubject(entity.getProviderSubject())
				.createdAt(entity.getCreatedAt())
				.lastUsedAt(entity.getLastUsedAt())
				.build();
	}

	public static IdentityEntity toEntity(Identity identity) {
		if (identity == null) return null;

		IdentityEntity entity = new IdentityEntity();
		entity.setUniqueId(identity.getUniqueId());
		entity.setProviderId(identity.getProviderId());
		entity.setProviderSubject(identity.getProviderSubject());
		entity.setCreatedAt(identity.getCreatedAt());
		entity.setLastUsedAt(identity.getLastUsedAt());
		return entity;
	}
}
