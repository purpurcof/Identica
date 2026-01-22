package me.whereareiam.identica.adapter.database.mapper;

import me.whereareiam.identica.adapter.database.entity.AccountLinkEntity;
import me.whereareiam.identica.model.account.AccountLink;

public final class AccountLinkMapper {
	public static AccountLink toModel(AccountLinkEntity entity) {
		if (entity == null) return null;

		return AccountLink.builder()
				.uniqueId(entity.getUniqueId())
				.providerId(entity.getProviderId())
				.primary(entity.isPrimary())
				.linkedAt(entity.getLinkedAt())
				.lastUsedAt(entity.getLastUsedAt())
				.build();
	}

	public static AccountLinkEntity toEntity(AccountLink link) {
		if (link == null) return null;

		return AccountLinkEntity.builder()
				.uniqueId(link.getUniqueId())
				.providerId(link.getProviderId())
				.primary(link.isPrimary())
				.linkedAt(link.getLinkedAt())
				.lastUsedAt(link.getLastUsedAt())
				.build();
	}
}
