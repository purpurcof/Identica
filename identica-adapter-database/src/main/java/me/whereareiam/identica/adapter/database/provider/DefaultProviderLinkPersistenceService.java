package me.whereareiam.identica.adapter.database.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.database.entity.AccountProviderLinkEntity;
import me.whereareiam.identica.adapter.database.mapper.AccountProviderLinkMapper;
import me.whereareiam.identica.adapter.database.repository.provider.ProviderLinkRepository;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.type.ClearScope;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DefaultProviderLinkPersistenceService implements ProviderLinkPersistenceService, EventListener {
	private final ProviderLinkRepository repository;
	private final EventManager eventManager;

	@Inject
	void registerListeners() {
		eventManager.register(this);
	}

	@Override
	public @NotNull Optional<AccountProviderLink> findBySubject(
			@NotNull String providerId, @NotNull String providerSubject
	) {
		if (providerId.isBlank() || providerSubject.isBlank()) return Optional.empty();
		return repository.findBySubject(providerId, providerSubject).map(AccountProviderLinkMapper::toModel);
	}

	@Override
	public @NotNull Optional<AccountProviderLink> findByUniqueIdAndProviderId(@NotNull UUID uniqueId, @NotNull String providerId) {
		if (providerId.isBlank()) return Optional.empty();
		return repository.findByUniqueIdAndProviderId(uniqueId, providerId).map(AccountProviderLinkMapper::toModel);
	}

	@Override
	public @NotNull List<AccountProviderLink> findByUniqueId(@NotNull UUID uniqueId) {
		return repository.findByUniqueId(uniqueId).stream()
				.map(AccountProviderLinkMapper::toModel)
				.toList();
	}

	@Override
	public @NotNull AccountProviderLink upsert(@NotNull AccountProviderLink link) {
		if (link.getProviderId().isBlank())
			throw new IllegalArgumentException("Provider id is required");
		if (link.getProviderSubject().isBlank())
			throw new IllegalArgumentException("Provider subject is required");

		AccountProviderLinkEntity entity = AccountProviderLinkMapper.toEntity(link);
		Optional<AccountProviderLinkEntity> existing = repository.findBySubject(link.getProviderId(), link.getProviderSubject());
		if (existing.isPresent()) {
			repository.update(link.getProviderId(), link.getProviderSubject(), link.isPrimary(), link.getLastSeenAt());
			if (link.isPrimary()) {
				repository.updatePrimary(link.getUniqueId(), link.getProviderId(), true);
			}
			return AccountProviderLinkMapper.toModel(existing.get().toBuilder()
					.primary(link.isPrimary())
					.lastSeenAt(link.getLastSeenAt())
					.build());
		}

		repository.insert(
				entity.getUniqueId(),
				entity.getProviderId(),
				entity.getProviderSubject(),
				entity.isPrimary(),
				entity.getLinkedAt(),
				entity.getLastSeenAt()
		);

		if (link.isPrimary()) {
			repository.updatePrimary(link.getUniqueId(), link.getProviderId(), true);
		}

		return AccountProviderLinkMapper.toModel(entity);
	}

	@Override
	public void deleteAll(@NotNull UUID uniqueId) {
		repository.deleteAll(uniqueId);
	}

	@IdenticEvent(EventOrder.HIGH)
	public void onAccountClear(@NotNull AccountClearEvent event) {
		if (event.getScope() != ClearScope.ALL)
			return;

		UUID uniqueId = event.getIdentity().getUniqueId();
		if (uniqueId == null)
			return;

		deleteAll(uniqueId);
	}
}
