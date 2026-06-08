package me.whereareiam.identica.feature.verification.database.repository;

import me.whereareiam.identica.feature.verification.database.entity.VerificationSelectionEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(VerificationSelectionEntity.class)
public interface VerificationSelectionRepository {
	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       method_id AS methodId,
			       selected_at AS selectedAt
			  FROM identica_verification_selections
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	Optional<VerificationSelectionEntity> find(@Bind("uniqueId") UUID uniqueId, @Bind("providerId") String providerId);

	@SqlQuery("""
			SELECT unique_id AS uniqueId,
			       provider_id AS providerId,
			       method_id AS methodId,
			       selected_at AS selectedAt
			  FROM identica_verification_selections
			 WHERE unique_id = :uniqueId
			""")
	List<VerificationSelectionEntity> findByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			INSERT INTO identica_verification_selections (
				unique_id, provider_id, method_id, selected_at
			) VALUES (
				:uniqueId, :providerId, :methodId, :selectedAt
			)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("methodId") String methodId,
			@Bind("selectedAt") long selectedAt
	);

	@SqlUpdate("""
			UPDATE identica_verification_selections
			   SET method_id = :methodId,
			       selected_at = :selectedAt
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	void update(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("methodId") String methodId,
			@Bind("selectedAt") long selectedAt
	);

	@SqlUpdate("""
			DELETE FROM identica_verification_selections
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			""")
	void delete(@Bind("uniqueId") UUID uniqueId, @Bind("providerId") String providerId);

	@SqlUpdate("""
			DELETE FROM identica_verification_selections
			 WHERE unique_id = :uniqueId
			   AND provider_id = :providerId
			   AND method_id = :methodId
			""")
	void deleteByMethod(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("providerId") String providerId,
			@Bind("methodId") String methodId
	);

	@SqlUpdate("""
			DELETE FROM identica_verification_selections
			 WHERE unique_id = :uniqueId
			   AND method_id = :methodId
			""")
	void deleteSelectionsByMethod(@Bind("uniqueId") UUID uniqueId, @Bind("methodId") String methodId);

	@SqlUpdate("""
			DELETE FROM identica_verification_selections
			 WHERE unique_id = :uniqueId
			""")
	void deleteAll(@Bind("uniqueId") UUID uniqueId);
}
