package me.whereareiam.identica.adapter.database.repository.verification;

import me.whereareiam.identica.adapter.database.entity.verification.VerificationEnrollmentEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(VerificationEnrollmentEntity.class)
public interface VerificationEnrollmentRepository {
	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       method_id AS methodId,
			       payload AS payload,
			       created_at AS createdAt,
			       enabled_at AS enabledAt
			  FROM identica_verification_enrollments
			 WHERE unique_id = :uniqueId
			   AND method_id = :methodId
			""")
	Optional<VerificationEnrollmentEntity> find(@Bind("uniqueId") UUID uniqueId, @Bind("methodId") String methodId);

	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       method_id AS methodId,
			       payload AS payload,
			       created_at AS createdAt,
			       enabled_at AS enabledAt
			  FROM identica_verification_enrollments
			 WHERE unique_id = :uniqueId
			""")
	List<VerificationEnrollmentEntity> findByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			INSERT INTO identica_verification_enrollments (
				unique_id, method_id, payload, created_at, enabled_at
			) VALUES (
				:uniqueId, :methodId, :payload, :createdAt, :enabledAt
			)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("methodId") String methodId,
			@Bind("payload") String payload,
			@Bind("createdAt") long createdAt,
			@Bind("enabledAt") long enabledAt
	);

	@SqlUpdate("""
			UPDATE identica_verification_enrollments
			   SET payload = :payload,
			       created_at = :createdAt,
			       enabled_at = :enabledAt
			 WHERE unique_id = :uniqueId
			   AND method_id = :methodId
			""")
	void update(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("methodId") String methodId,
			@Bind("payload") String payload,
			@Bind("createdAt") long createdAt,
			@Bind("enabledAt") long enabledAt
	);

	@SqlUpdate("""
			DELETE FROM identica_verification_enrollments
			 WHERE unique_id = :uniqueId
			   AND method_id = :methodId
			""")
	void delete(@Bind("uniqueId") UUID uniqueId, @Bind("methodId") String methodId);

	@SqlUpdate("""
			DELETE FROM identica_verification_enrollments
			 WHERE unique_id = :uniqueId
			""")
	void deleteAll(@Bind("uniqueId") UUID uniqueId);
}
