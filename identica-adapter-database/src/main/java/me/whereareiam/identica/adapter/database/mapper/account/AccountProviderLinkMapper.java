package me.whereareiam.identica.adapter.database.mapper.account;

import me.whereareiam.identica.adapter.database.entity.account.AccountProviderLinkEntity;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;

public final class AccountProviderLinkMapper {
	public static AccountProviderLink toModel(AccountProviderLinkEntity entity) {
		if (entity == null) return null;
		return AccountProviderLink.builder()
				.uniqueId(entity.getUniqueId())
				.providerId(entity.getProviderId())
				.providerSubject(entity.getProviderSubject())
				.primaryLink(entity.isPrimaryLink())
				.linkedAt(entity.getLinkedAt())
				.lastSeenAt(entity.getLastSeenAt())
				.build();
	}

	public static AccountProviderLinkEntity toEntity(AccountProviderLink link) {
		if (link == null) return null;
		return AccountProviderLinkEntity.builder()
				.uniqueId(link.getUniqueId())
				.providerId(link.getProviderId())
				.providerSubject(link.getProviderSubject())
				.primaryLink(link.isPrimaryLink())
				.linkedAt(link.getLinkedAt())
				.lastSeenAt(link.getLastSeenAt())
				.build();
	}
}
