package me.whereareiam.identica.adapter.database.repository.account;

import me.whereareiam.identica.adapter.database.entity.AccountEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(AccountEntity.class)
public interface AccountRepository {
	@SqlQuery("""
			SELECT uuid AS uniqueId,
			       created_at AS createdAt,
			       last_seen_at AS lastSeenAt
			  FROM identica_accounts
			 WHERE uuid = :uniqueId
			""")
	Optional<AccountEntity> findByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			INSERT INTO identica_accounts (uuid, created_at, last_seen_at)
			VALUES (:uniqueId, :createdAt, :lastSeenAt)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("createdAt") long createdAt,
			@Bind("lastSeenAt") long lastSeenAt
	);

	@SqlUpdate("""
			UPDATE identica_accounts
			   SET last_seen_at = :lastSeenAt
			 WHERE uuid = :uniqueId
			""")
	void updateLastSeen(@Bind("uniqueId") UUID uniqueId, @Bind("lastSeenAt") long lastSeenAt);

	@SqlUpdate("DELETE FROM identica_accounts WHERE uuid = :uniqueId")
	void delete(@Bind("uniqueId") UUID uniqueId);

	@SqlQuery("SELECT COUNT(*) > 0 FROM identica_accounts WHERE uuid = :uniqueId")
	boolean exists(@Bind("uniqueId") UUID uniqueId);

	default void save(AccountEntity entity) {
		if (exists(entity.getUniqueId())) {
			updateLastSeen(entity.getUniqueId(), entity.getLastSeenAt());
			return;
		}

		insert(entity.getUniqueId(), entity.getCreatedAt(), entity.getLastSeenAt());
	}
}
