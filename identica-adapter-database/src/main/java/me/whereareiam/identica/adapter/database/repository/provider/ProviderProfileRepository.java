package me.whereareiam.identica.adapter.database.repository.provider;

import me.whereareiam.identica.adapter.database.entity.AccountProviderProfileEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

@RegisterBeanMapper(AccountProviderProfileEntity.class)
public interface ProviderProfileRepository {
	@SqlQuery("""
			SELECT provider_id AS providerId,
			       provider_subject AS providerSubject,
			       provider_username AS providerUsername
			  FROM identica_provider_profiles
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	Optional<AccountProviderProfileEntity> findBySubject(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);

	@SqlUpdate("""
			INSERT INTO identica_provider_profiles (
				provider_id, provider_subject, provider_username
			) VALUES (
				:providerId, :providerSubject, :providerUsername
			)
			""")
	void insert(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("providerUsername") String providerUsername
	);

	@SqlUpdate("""
			UPDATE identica_provider_profiles
			   SET provider_username = :providerUsername
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	void update(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("providerUsername") String providerUsername
	);

	@SqlUpdate("""
			DELETE FROM identica_provider_profiles
			 WHERE provider_id = :providerId
			   AND provider_subject = :providerSubject
			""")
	void delete(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject
	);
}
