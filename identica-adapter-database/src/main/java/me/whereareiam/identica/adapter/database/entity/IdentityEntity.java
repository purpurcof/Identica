package me.whereareiam.identica.adapter.database.entity;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;

import java.util.UUID;

@Getter
@Setter
@Entity(tableName = "identica_identities")
public class IdentityEntity implements EntitySchemaProvider {
	private UUID uniqueId;
	private String providerId;
	private String providerSubject;
	private long createdAt;
	private long lastUsedAt;

	@Override
	public String statement(String databaseType) {
		String type = databaseType == null ? "" : databaseType.toUpperCase();
		String uuidType = switch (type) {
			case "POSTGRES" -> "UUID";
			case "SQLITE" -> "TEXT";
			default -> "CHAR(36)";
		};
		String timeType = switch (type) {
			case "SQLITE" -> "INTEGER";
			default -> "BIGINT";
		};
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
