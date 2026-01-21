package me.whereareiam.identica.adapter.database.repository.link;

import me.whereareiam.identica.adapter.database.entity.AccountLinkEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(AccountLinkEntity.class)
public interface AccountLinkRepository {
	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       is_primary AS "primary",
			       linked_at AS linkedAt,
			       last_used_at AS lastUsedAt
			  FROM account_links
			 WHERE unique_id = :uniqueId
			""")
	List<AccountLinkEntity> findByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       is_primary AS "primary",
			       linked_at AS linkedAt,
			       last_used_at AS lastUsedAt
			  FROM identica_account_links
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	Optional<AccountLinkEntity> findByUniqueIdAndProviderId(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId
	);

	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       is_primary AS "primary",
			       linked_at AS linkedAt,
			       last_used_at AS lastUsedAt
			  FROM identica_account_links
			 WHERE unique_id = :uniqueId
			   AND is_primary = TRUE
			""")
	Optional<AccountLinkEntity> findPrimary(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			INSERT INTO identica_account_links (
				unique_id, provider_id, is_primary, linked_at, last_used_at
			) VALUES (
				:uniqueId, :providerId, :primary, :linkedAt, :lastUsedAt
			)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("primary") boolean primary,
			@Bind("linkedAt") long linkedAt,
			@Bind("lastUsedAt") long lastUsedAt
	);

	@SqlUpdate("""
			UPDATE identica_account_links
			   SET last_used_at = :lastUsedAt
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	void updateLastUsed(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("lastUsedAt") long lastUsedAt
	);

	@SqlUpdate("""
			UPDATE identica_account_links
			   SET is_primary = FALSE
			 WHERE unique_id = :uniqueId
			""")
	void clearPrimary(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			UPDATE identica_account_links
			   SET is_primary = TRUE
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	void setPrimary(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId
	);

	@SqlUpdate("""
			DELETE FROM identica_account_links
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	void delete(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId
	);

	@SqlQuery("""
			SELECT COUNT(*) > 0
			  FROM identica_account_links
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	boolean exists(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId
	);
}
