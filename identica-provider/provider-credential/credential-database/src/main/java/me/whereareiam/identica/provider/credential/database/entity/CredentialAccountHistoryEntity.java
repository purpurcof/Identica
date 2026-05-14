package me.whereareiam.identica.provider.credential.database.entity;

import lombok.*;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(tableName = "identica_provider_credential_accounts_history")
public class CredentialAccountHistoryEntity implements EntitySchemaProvider {
	private String providerId;
	private String providerSubject;
	private String hashingMethod;
	private String changeReason;
	private long changedAt;

	@Override
	public String statement(String databaseType) {
		String type = databaseType == null ? "" : databaseType.toUpperCase();
		String timeType = switch (type) {
			case "SQLITE" -> "INTEGER";
			default -> "BIGINT";
		};

		return """
				CREATE TABLE IF NOT EXISTS identica_provider_credential_accounts_history (
					provider_id VARCHAR(64) NOT NULL,
					provider_subject VARCHAR(128) NOT NULL,
					hashing_method VARCHAR(64) NOT NULL,
					change_reason VARCHAR(32) NOT NULL,
					changed_at %s,
					PRIMARY KEY (provider_id, provider_subject, changed_at),
					FOREIGN KEY (provider_id, provider_subject)
						REFERENCES identica_provider_credential_accounts(provider_id, provider_subject)
						ON DELETE CASCADE
				)
				""".formatted(timeType);
	}
}
