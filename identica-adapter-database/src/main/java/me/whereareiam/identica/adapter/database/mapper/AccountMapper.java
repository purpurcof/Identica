package me.whereareiam.identica.adapter.database.mapper;

import me.whereareiam.identica.adapter.database.entity.AccountEntity;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.type.UsernameSource;

public final class AccountMapper {
	public static Account toModel(AccountEntity entity) {
		if (entity == null) return null;
		return Account.builder()
				.uniqueId(entity.getUniqueId())
				.username(entity.getUsername())
				.source(UsernameSource.fromId(entity.getUsernameSource()))
				.createdAt(entity.getCreatedAt())
				.lastSeenAt(entity.getLastSeenAt())
				.build();
	}

	public static AccountEntity toEntity(Account account) {
		if (account == null) return null;
		return AccountEntity.builder()
				.uniqueId(account.getUniqueId())
				.username(account.getUsername())
				.usernameSource(account.getSource().getId())
				.createdAt(account.getCreatedAt())
				.lastSeenAt(account.getLastSeenAt())
				.build();
	}
}
