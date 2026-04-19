package me.whereareiam.identica.adapter.database.entity.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(tableName = "identica_account_provider_profiles")
public class AccountProviderProfileEntity implements EntitySchemaProvider {
	private String providerId;
	private String providerSubject;
	private String providerUsername;

	@Override
	public String statement(String databaseType) {
		return """
				CREATE TABLE IF NOT EXISTS identica_provider_profiles (
					provider_id VARCHAR(64) NOT NULL,
					provider_subject VARCHAR(128) NOT NULL,
					provider_username VARCHAR(64) NOT NULL,
					PRIMARY KEY (provider_id, provider_subject),
					FOREIGN KEY (provider_id, provider_subject) REFERENCES identica_provider_links(provider_id, provider_subject) ON DELETE CASCADE
				)
				""";
	}
}
