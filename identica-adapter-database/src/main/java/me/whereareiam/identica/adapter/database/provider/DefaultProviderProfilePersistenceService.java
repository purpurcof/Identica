package me.whereareiam.identica.adapter.database.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.database.entity.account.AccountProviderProfileEntity;
import me.whereareiam.identica.adapter.database.mapper.account.AccountProviderProfileMapper;
import me.whereareiam.identica.adapter.database.repository.provider.ProviderProfileRepository;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DefaultProviderProfilePersistenceService implements ProviderProfilePersistenceService {
	private final ProviderProfileRepository repository;

	@Override
	public @NotNull Optional<AccountProviderProfile> findBySubject(
			@NotNull String providerId,
			@NotNull String providerSubject
	) {
		if (providerId.isBlank() || providerSubject.isBlank()) return Optional.empty();
		return repository.findBySubject(providerId, providerSubject).map(AccountProviderProfileMapper::toModel);
	}

	@Override
	public @NotNull AccountProviderProfile upsert(@NotNull AccountProviderProfile profile) {
		if (profile.getProviderId().isBlank())
			throw new IllegalArgumentException("Provider resolver provider id is required");
		if (profile.getProviderSubject().isBlank())
			throw new IllegalArgumentException("Provider resolver provider subject is required");

		AccountProviderProfileEntity entity = AccountProviderProfileMapper.toEntity(profile);
		Optional<AccountProviderProfileEntity> existing = repository.findBySubject(
				profile.getProviderId(),
				profile.getProviderSubject()
		);
		if (existing.isPresent()) {
			repository.update(
					entity.getProviderId(),
					entity.getProviderSubject(),
					entity.getProviderUsername()
			);
			return AccountProviderProfileMapper.toModel(entity);
		}

		repository.insert(
				entity.getProviderId(),
				entity.getProviderSubject(),
				entity.getProviderUsername()
		);

		return AccountProviderProfileMapper.toModel(entity);
	}

	@Override
	public void delete(@NotNull String providerId, @NotNull String providerSubject) {
		if (providerId.isBlank() || providerSubject.isBlank()) return;
		repository.delete(providerId, providerSubject);
	}
}

