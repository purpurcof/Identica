package me.whereareiam.identica.provider.capability.authoritative.username.database.mapper;

import me.whereareiam.identica.provider.capability.authoritative.username.database.entity.AuthoritativeUsernameHistoryEntity;
import me.whereareiam.identica.provider.capability.authoritative.username.model.account.AccountUsernameHistoryEntry;

public final class AuthoritativeUsernameHistoryMapper {
	public static AuthoritativeUsernameHistoryEntity toEntity(AccountUsernameHistoryEntry entry) {
		if (entry == null) return null;
		return AuthoritativeUsernameHistoryEntity.builder()
				.uniqueId(entry.getUniqueId())
				.providerId(entry.getProviderId())
				.oldUsername(entry.getOldUsername())
				.newUsername(entry.getNewUsername())
				.source(entry.getSource().getId())
				.changedAt(entry.getChangedAt())
				.build();
	}
}
