package me.whereareiam.identica.adapter.database.repository.account;

import me.whereareiam.identica.adapter.database.entity.AccountEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(AccountEntity.class)
public interface AccountRepository {
	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       username AS username,
			       username_source AS usernameSource,
			       created_at AS createdAt,
			       last_seen_at AS lastSeenAt
			  FROM identica_accounts
			 WHERE unique_id = :uniqueId
			""")
	Optional<AccountEntity> findByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       username AS username,
			       username_source AS usernameSource,
			       created_at AS createdAt,
			       last_seen_at AS lastSeenAt
			  FROM identica_accounts
			 WHERE LOWER(username) = LOWER(:username)
			""")
	List<AccountEntity> findByUsername(@Bind("username") String username);

	@SqlUpdate("""
			INSERT INTO identica_accounts (unique_id, username, username_source, created_at, last_seen_at)
			VALUES (:uniqueId, :username, :usernameSource, :createdAt, :lastSeenAt)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("username") String username,
			@Bind("usernameSource") String usernameSource,
			@Bind("createdAt") long createdAt,
			@Bind("lastSeenAt") long lastSeenAt
	);

	@SqlUpdate("""
			UPDATE identica_accounts
			   SET last_seen_at = :lastSeenAt
			 WHERE unique_id = :uniqueId
			""")
	void updateLastSeen(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("lastSeenAt") long lastSeenAt
	);

	@SqlUpdate("""
			UPDATE identica_accounts
			   SET username = :username
			 WHERE unique_id = :uniqueId
			""")
	void updateUsername(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("username") String username
	);

	@SqlUpdate("""
			UPDATE identica_accounts
			   SET username_source = :usernameSource
			 WHERE unique_id = :uniqueId
			""")
	void updateUsernameSource(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("usernameSource") String usernameSource
	);

	@SqlUpdate("DELETE FROM identica_accounts WHERE unique_id = :uniqueId")
	void delete(@Bind("uniqueId") UUID uniqueId);
}
