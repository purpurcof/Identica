package me.whereareiam.identica.feature.verification.database.repository;

import me.whereareiam.identica.feature.verification.database.entity.VerificationRecoveryCodeEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.UUID;

@RegisterBeanMapper(VerificationRecoveryCodeEntity.class)
public interface VerificationRecoveryCodeRepository {
	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       method_id AS methodId,
			       code_hash AS codeHash,
			       created_at AS createdAt,
			       used_at AS usedAt
			  FROM identica_verification_recovery_codes
			 WHERE unique_id = :uniqueId
			   AND method_id = :methodId
			""")
	List<VerificationRecoveryCodeEntity> findByUniqueIdAndMethod(@Bind("uniqueId") UUID uniqueId, @Bind("methodId") String methodId);

	@SqlUpdate("""
			INSERT INTO identica_verification_recovery_codes (
				unique_id, method_id, code_hash, created_at, used_at
			) VALUES (
				:uniqueId, :methodId, :codeHash, :createdAt, :usedAt
			)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("methodId") String methodId,
			@Bind("codeHash") String codeHash,
			@Bind("createdAt") long createdAt,
			@Bind("usedAt") long usedAt
	);

	@SqlUpdate("""
			UPDATE identica_verification_recovery_codes
			   SET used_at = :usedAt
			 WHERE unique_id = :uniqueId
			   AND method_id = :methodId
			   AND code_hash = :codeHash
			   AND used_at = 0
			""")
	int markUsed(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("methodId") String methodId,
			@Bind("codeHash") String codeHash,
			@Bind("usedAt") long usedAt
	);

	@SqlUpdate("""
			DELETE FROM identica_verification_recovery_codes
			 WHERE unique_id = :uniqueId
			   AND method_id = :methodId
			""")
	void deleteByMethod(@Bind("uniqueId") UUID uniqueId, @Bind("methodId") String methodId);

	@SqlUpdate("""
			DELETE FROM identica_verification_recovery_codes
			 WHERE unique_id = :uniqueId
			""")
	void deleteAll(@Bind("uniqueId") UUID uniqueId);
}
