package me.whereareiam.identica.provider.credential.database.entity;

import lombok.*;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(tableName = "identica_provider_credential_accounts")
public class CredentialAccountEntity implements EntitySchemaProvider {
	private String providerId;
	private String providerSubject;
	private String passwordHash;
	private String hashingMethod;
	private long createdAt;
	private long updatedAt;

	@Override
	public String statement(String databaseType) {
		String type = databaseType == null ? "" : databaseType.toUpperCase();
		String timeType = switch (type) {
			case "SQLITE" -> "INTEGER";
			default -> "BIGINT";
		};

		return """
				CREATE TABLE IF NOT EXISTS identica_provider_credential_accounts (
					provider_id VARCHAR(64) NOT NULL,
					provider_subject VARCHAR(128) NOT NULL,
					password_hash VARCHAR(512) NOT NULL,
					hashing_method VARCHAR(64) NOT NULL,
					created_at %s,
					updated_at %s,
					PRIMARY KEY (provider_id, provider_subject)
				)
				""".formatted(timeType, timeType);
	}
}
