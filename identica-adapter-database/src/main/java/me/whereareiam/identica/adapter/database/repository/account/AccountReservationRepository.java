package me.whereareiam.identica.adapter.database.repository.account;

import me.whereareiam.identica.adapter.database.entity.account.AccountReservationEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(AccountReservationEntity.class)
public interface AccountReservationRepository {
	@SqlQuery("""
			SELECT reservation_key AS reservationKey,
			       unique_id AS uniqueId,
			       created_at AS createdAt,
			       expires_at AS expiresAt
			  FROM identica_account_reservations
			 WHERE reservation_key = :reservationKey
			""")
	Optional<AccountReservationEntity> find(@Bind("reservationKey") String reservationKey);

	@SqlUpdate("""
			DELETE FROM identica_account_reservations
			 WHERE reservation_key = :reservationKey
			""")
	void delete(@Bind("reservationKey") String reservationKey);

	@SqlUpdate("""
			DELETE FROM identica_account_reservations
			 WHERE unique_id = :uniqueId
			""")
	void deleteByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			DELETE FROM identica_account_reservations
			 WHERE expires_at <= :now
			""")
	void deleteExpired(@Bind("now") long now);

	@SqlUpdate("""
			MERGE INTO identica_account_reservations (
				reservation_key, unique_id, created_at, expires_at
			) KEY (reservation_key) VALUES (
				:reservationKey, :uniqueId, :createdAt, :expiresAt
			)
			""")
	void upsertH2(
			@Bind("reservationKey") String reservationKey,
			@Bind("uniqueId") UUID uniqueId,
			@Bind("createdAt") long createdAt,
			@Bind("expiresAt") long expiresAt
	);

	@SqlUpdate("""
			INSERT INTO identica_account_reservations (
				reservation_key, unique_id, created_at, expires_at
			) VALUES (
				:reservationKey, :uniqueId, :createdAt, :expiresAt
			)
			ON CONFLICT (reservation_key) DO UPDATE SET
				unique_id = excluded.unique_id,
				created_at = excluded.created_at,
				expires_at = excluded.expires_at
			""")
	void upsertPostgresSqlite(
			@Bind("reservationKey") String reservationKey,
			@Bind("uniqueId") UUID uniqueId,
			@Bind("createdAt") long createdAt,
			@Bind("expiresAt") long expiresAt
	);

	@SqlUpdate("""
			INSERT INTO identica_account_reservations (
				reservation_key, unique_id, created_at, expires_at
			) VALUES (
				:reservationKey, :uniqueId, :createdAt, :expiresAt
			)
			ON DUPLICATE KEY UPDATE
				unique_id = VALUES(unique_id),
				created_at = VALUES(created_at),
				expires_at = VALUES(expires_at)
			""")
	void upsertMysql(
			@Bind("reservationKey") String reservationKey,
			@Bind("uniqueId") UUID uniqueId,
			@Bind("createdAt") long createdAt,
			@Bind("expiresAt") long expiresAt
	);
}
