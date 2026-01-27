package me.whereareiam.identica.adapter.database.repository.provider;

import me.whereareiam.identica.adapter.database.entity.AccountProviderLinkEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(AccountProviderLinkEntity.class)
public interface ProviderLinkRepository {
	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       provider_subject AS providerSubject,
			       is_primary AS primary_flag,
			       linked_at AS linkedAt,
			       last_seen_at AS lastSeenAt
			  FROM identica_provider_links
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	Optional<AccountProviderLinkEntity> findBySubject(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);

	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       provider_subject AS providerSubject,
			       is_primary AS primary_flag,
			       linked_at AS linkedAt,
			       last_seen_at AS lastSeenAt
			  FROM identica_provider_links
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	Optional<AccountProviderLinkEntity> findByUniqueIdAndProviderId(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId
	);

	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       provider_subject AS providerSubject,
			       is_primary AS primary_flag,
			       linked_at AS linkedAt,
			       last_seen_at AS lastSeenAt
			  FROM identica_provider_links
			 WHERE unique_id = :uniqueId
			""")
	List<AccountProviderLinkEntity> findByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			INSERT INTO identica_provider_links (
				unique_id, provider_id, provider_subject, is_primary, linked_at, last_seen_at
			) VALUES (
				:uniqueId, :providerId, :providerSubject, :primary, :linkedAt, :lastSeenAt
			)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("primary") boolean primary,
			@Bind("linkedAt") long linkedAt,
			@Bind("lastSeenAt") long lastSeenAt
	);

	@SqlUpdate("""
			UPDATE identica_provider_links
			   SET is_primary = :primary,
			       last_seen_at = :lastSeenAt
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	void update(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("primary") boolean primary,
			@Bind("lastSeenAt") long lastSeenAt
	);

	@SqlUpdate("""
			UPDATE identica_provider_links
			   SET is_primary = :primary
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	void updatePrimary(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("primary") boolean primary
	);

	@SqlUpdate("""
			DELETE FROM identica_provider_links
			 WHERE unique_id = :uniqueId
			""")
	void deleteAll(@Bind("uniqueId") UUID uniqueId);
}
