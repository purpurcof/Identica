package me.whereareiam.identica.provider.capability.authoritative.username.database.mapper;

import me.whereareiam.identica.provider.capability.authoritative.username.database.entity.AuthoritativeUsernameStateEntity;
import me.whereareiam.identica.provider.capability.authoritative.username.model.account.AccountUsernameState;
import me.whereareiam.identica.provider.capability.authoritative.username.type.AccountUsernameSource;

public final class AuthoritativeUsernameStateMapper {
	public static AccountUsernameState toModel(AuthoritativeUsernameStateEntity entity) {
		if (entity == null) return null;
		return AccountUsernameState.builder()
				.uniqueId(entity.getUniqueId())
				.source(AccountUsernameSource.fromId(entity.getSource()))
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
