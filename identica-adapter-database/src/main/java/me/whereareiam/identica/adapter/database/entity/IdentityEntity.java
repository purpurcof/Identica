package me.whereareiam.identica.adapter.database.entity;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.type.DatabaseType;

@Getter
@Setter
@Entity(tableName = "identica_identities")
public class IdentityEntity implements EntitySchemaProvider {
	private String uuid;
	private String providerId;
	private String providerSubject;
	private long createdAt;
	private long lastUsedAt;

	@Override
	public String statement(String databaseType) {
		String uuidType = DatabaseType.POSTGRES.equals(databaseType) ? "UUID"
				: DatabaseType.SQLITE.equals(databaseType) ? "TEXT" : "CHAR(36)";
		String timeType = DatabaseType.SQLITE.equals(databaseType) ? "INTEGER" : "BIGINT";
		return """
				CREATE TABLE IF NOT EXISTS identica_identities (
					uuid %s NOT NULL,
					provider_id VARCHAR(64) NOT NULL,
					provider_subject VARCHAR(128) NOT NULL,
					created_at %s,
					last_used_at %s,
					UNIQUE (provider_id, provider_subject)
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
