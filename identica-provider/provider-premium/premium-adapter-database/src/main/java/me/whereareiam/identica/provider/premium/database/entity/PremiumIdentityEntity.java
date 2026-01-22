package me.whereareiam.identica.provider.premium.database.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(tableName = "identica_premium_identities")
public class PremiumIdentityEntity implements EntitySchemaProvider {
	private UUID identicaUniqueId;
	private UUID mojangUniqueId;

	@Override
	public String statement(String databaseType) {
		String type = databaseType == null ? "" : databaseType.toUpperCase();
		String uuidType = switch (type) {
			case "POSTGRES" -> "UUID";
			case "SQLITE" -> "TEXT";
			default -> "CHAR(36)";
		};
		return """
				CREATE TABLE IF NOT EXISTS identica_premium_identities (
					unique_id %s NOT NULL,
					mojang_unique_id %s NOT NULL,
					UNIQUE (unique_id),
					UNIQUE (mojang_unique_id)
				)
				""".formatted(uuidType, uuidType);
	}
}
