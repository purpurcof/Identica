package me.whereareiam.identica.adapter.database.username;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.database.entity.UsernameHistoryEntity;
import me.whereareiam.identica.adapter.database.mapper.UsernameHistoryMapper;
import me.whereareiam.identica.adapter.database.repository.username.UsernameHistoryRepository;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.model.UsernameHistoryEntry;
import org.jetbrains.annotations.NotNull;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultUsernameHistoryPersistenceService implements UsernameHistoryPersistenceService {
	private final UsernameHistoryRepository repository;

	@Override
	public void record(@NotNull UsernameHistoryEntry entry) {
		if (entry.getOldUsername().isBlank())
			throw new IllegalArgumentException("Username history old username is required");
		if (entry.getNewUsername().isBlank())
			throw new IllegalArgumentException("Username history new username is required");
		if (entry.getSource().isBlank())
			throw new IllegalArgumentException("Username history source is required");

		UsernameHistoryEntity entity = UsernameHistoryMapper.toEntity(entry);
		repository.insert(
				entity.getUniqueId(),
				entity.getProviderId(),
				entity.getOldUsername(),
				entity.getNewUsername(),
				entity.getSource(),
				entity.getChangedAt()
		);
	}
}
