package me.whereareiam.identica.adapter.database.username;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.adapter.database.entity.UsernameHistoryEntity;
import me.whereareiam.identica.adapter.database.mapper.UsernameHistoryMapper;
import me.whereareiam.identica.adapter.database.repository.username.UsernameHistoryRepository;
import me.whereareiam.identica.database.UsernameHistoryPersistenceService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountLifecycleEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.UsernameHistoryEntry;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Singleton
public class DefaultUsernameHistoryPersistenceService implements UsernameHistoryPersistenceService, EventListener {
	private final UsernameHistoryRepository repository;

	@Inject
	public DefaultUsernameHistoryPersistenceService(
			@NotNull UsernameHistoryRepository repository,
			@NotNull EventManager eventManager
	) {
		this.repository = repository;
		eventManager.register(this);
	}

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

	@Override
	public void deleteAll(@NotNull UUID uniqueId) {
		repository.deleteAll(uniqueId);
	}

	@IdenticEvent(EventOrder.HIGH)
	public void onAccountLifecycle(@NotNull AccountLifecycleEvent event) {
		UUID uniqueId = event.getIdentity().getUniqueId();
		if (uniqueId == null) return;

		deleteAll(uniqueId);
	}
}
