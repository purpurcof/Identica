package me.whereareiam.identica.adapter.database.repository.identity;

import me.whereareiam.identica.adapter.database.entity.IdentityEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(IdentityEntity.class)
public interface IdentityRepository {
	@SqlQuery("""
			SELECT uuid AS uniqueId,
			       provider_id AS providerId,
			       provider_subject AS providerSubject,
			       created_at AS createdAt,
			       last_used_at AS lastUsedAt
			  FROM identica_identities
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	Optional<IdentityEntity> findByProvider(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);

	@SqlQuery("""
			SELECT uuid AS uniqueId,
			       provider_id AS providerId,
			       provider_subject AS providerSubject,
			       created_at AS createdAt,
			       last_used_at AS lastUsedAt
			  FROM identica_identities
			 WHERE uuid = :uniqueId
			""")
	List<IdentityEntity> findByAccount(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			INSERT INTO identica_identities (
				uuid, provider_id, provider_subject, created_at, last_used_at
			) VALUES (
				:uniqueId, :providerId, :providerSubject, :createdAt, :lastUsedAt
			)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("createdAt") long createdAt,
			@Bind("lastUsedAt") long lastUsedAt
	);

	@SqlUpdate("""
			UPDATE identica_identities
			   SET last_used_at = :lastUsedAt
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	void updateLastUsed(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("lastUsedAt") long lastUsedAt
	);

	@SqlUpdate("""
			DELETE FROM identica_identities
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	void delete(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);

	@SqlQuery("""
			SELECT COUNT(*) > 0
			  FROM identica_identities
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	boolean exists(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);

	default void save(IdentityEntity entity) {
		if (exists(entity.getProviderId(), entity.getProviderSubject())) {
			updateLastUsed(entity.getProviderId(), entity.getProviderSubject(), entity.getLastUsedAt());
			return;
		}

		insert(entity.getUniqueId(), entity.getProviderId(), entity.getProviderSubject(),
				entity.getCreatedAt(), entity.getLastUsedAt());
	}
}
