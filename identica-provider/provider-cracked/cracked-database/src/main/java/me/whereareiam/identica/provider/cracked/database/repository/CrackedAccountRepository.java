package me.whereareiam.identica.provider.cracked.database.repository;

import me.whereareiam.identica.provider.cracked.database.entity.CrackedAccountEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

@RegisterBeanMapper(CrackedAccountEntity.class)
public interface CrackedAccountRepository {
	@SqlQuery("""
			SELECT provider_id AS providerId,
			       provider_subject AS providerSubject,
			       password_hash AS passwordHash,
			       hashing_method AS hashingMethod,
			       created_at AS createdAt,
			       updated_at AS updatedAt
			  FROM identica_cracked_accounts
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	Optional<CrackedAccountEntity> findBySubject(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);

	@SqlUpdate("""
			INSERT INTO identica_cracked_accounts
				(provider_id, provider_subject, password_hash, hashing_method, created_at, updated_at)
			VALUES
				(:providerId, :providerSubject, :passwordHash, :hashingMethod, :createdAt, :updatedAt)
			""")
	void insert(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("passwordHash") String passwordHash,
			@Bind("hashingMethod") String hashingMethod,
			@Bind("createdAt") long createdAt,
			@Bind("updatedAt") long updatedAt
	);

	@SqlUpdate("""
			UPDATE identica_cracked_accounts
			   SET password_hash = :passwordHash,
			       hashing_method = :hashingMethod,
			       updated_at = :updatedAt
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	void updatePassword(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("passwordHash") String passwordHash,
			@Bind("hashingMethod") String hashingMethod,
			@Bind("updatedAt") long updatedAt
	);

	@SqlUpdate("""
			DELETE FROM identica_cracked_accounts
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	void delete(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);
}
