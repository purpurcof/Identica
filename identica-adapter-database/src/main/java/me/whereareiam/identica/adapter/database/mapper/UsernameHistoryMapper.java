package me.whereareiam.identica.adapter.database.mapper;

import me.whereareiam.identica.adapter.database.entity.UsernameHistoryEntity;
import me.whereareiam.identica.model.UsernameHistoryEntry;

public final class UsernameHistoryMapper {
	public static UsernameHistoryEntry toModel(UsernameHistoryEntity entity) {
		if (entity == null) return null;
		return UsernameHistoryEntry.builder()
				.uniqueId(entity.getUniqueId())
				.providerId(entity.getProviderId())
				.oldUsername(entity.getOldUsername())
				.newUsername(entity.getNewUsername())
				.source(entity.getSource())
				.changedAt(entity.getChangedAt())
				.build();
	}

	public static UsernameHistoryEntity toEntity(UsernameHistoryEntry entry) {
		if (entry == null) return null;
		return UsernameHistoryEntity.builder()
				.uniqueId(entry.getUniqueId())
				.providerId(entry.getProviderId())
				.oldUsername(entry.getOldUsername())
				.newUsername(entry.getNewUsername())
				.source(entry.getSource())
				.changedAt(entry.getChangedAt())
				.build();
	}
}
