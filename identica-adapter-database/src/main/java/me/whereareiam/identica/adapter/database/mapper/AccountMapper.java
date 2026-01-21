package me.whereareiam.identica.adapter.database.mapper;

import me.whereareiam.identica.adapter.database.entity.AccountEntity;
import me.whereareiam.identica.model.Account;

public final class AccountMapper {
	public static Account toModel(AccountEntity entity) {
		if (entity == null) return null;

		return Account.builder()
				.uniqueId(entity.getUniqueId())
				.createdAt(entity.getCreatedAt())
				.lastSeenAt(entity.getLastSeenAt())
				.build();
	}

	public static AccountEntity toEntity(Account account) {
		if (account == null) return null;

		return AccountEntity.builder()
				.uniqueId(account.getUniqueId())
				.createdAt(account.getCreatedAt())
				.lastSeenAt(account.getLastSeenAt())
				.build();
	}
}
