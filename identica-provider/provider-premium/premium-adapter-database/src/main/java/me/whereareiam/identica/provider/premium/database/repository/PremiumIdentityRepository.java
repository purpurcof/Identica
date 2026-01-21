package me.whereareiam.identica.provider.premium.database.repository;

import me.whereareiam.identica.provider.premium.database.entity.PremiumIdentityEntity;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(PremiumIdentityEntity.class)
public interface PremiumIdentityRepository {
	@SqlQuery("""
			SELECT identica_unique_id AS uniqueId,
			       mojang_unique_id AS mojangUniqueId
			  FROM identica_premium_identities
			 WHERE mojang_unique_id = :mojangUniqueId
			""")
	Optional<PremiumIdentityEntity> findByMojangUniqueId(@Bind("mojangUniqueId") UUID mojangUniqueId);

	@SqlQuery("""
			SELECT identica_unique_id AS uniqueId,
			       mojang_unique_id AS mojangUniqueId
			  FROM identica_premium_identities
			 WHERE identica_unique_id = :uniqueId
			""")
	Optional<PremiumIdentityEntity> findByUniqueId(@Bind("uniqueId") UUID uniqueId);

	@SqlUpdate("""
			DELETE FROM identica_premium_identities
			 WHERE identica_unique_id = :uniqueId
			    OR mojang_unique_id = :mojangUniqueId
			""")
	void deleteByIdenticaOrMojang(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("mojangUniqueId") UUID mojangUniqueId
	);

	@SqlUpdate("""
			INSERT INTO identica_premium_identities (identica_unique_id, mojang_unique_id)
			VALUES (:uniqueId, :mojangUniqueId)
			""")
	void insert(
			@Bind("uniqueId") UUID uniqueId,
			@Bind("mojangUniqueId") UUID mojangUniqueId
	);
}
