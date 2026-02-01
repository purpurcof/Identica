package me.whereareiam.identica.adapter.database.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(tableName = "identica_account_provider_links")
public class AccountProviderLinkEntity implements EntitySchemaProvider {
	private UUID uniqueId;

	private String providerId;
	private String providerSubject;

	@ColumnName("primary_flag")
	private boolean primary;
	private long linkedAt;
	private long lastSeenAt;

	@Override
	public String statement(String databaseType) {
		String type = databaseType == null ? "" : databaseType.toUpperCase();
		String uuidType = switch (type) {
			case "POSTGRES" -> "UUID";
			case "H2" -> "UUID";
			case "SQLITE" -> "TEXT";
			default -> "CHAR(36)";
		};

		String timeType = switch (type) {
			case "SQLITE" -> "INTEGER";
			default -> "BIGINT";
		};

		String booleanType = switch (type) {
			case "SQLITE" -> "INTEGER";
			default -> "BOOLEAN";
		};

		return """
				CREATE TABLE IF NOT EXISTS identica_provider_links (
					provider_id VARCHAR(64) NOT NULL,
					provider_subject VARCHAR(128) NOT NULL,
					unique_id %s NOT NULL,
					is_primary %s NOT NULL,
					linked_at %s,
					last_seen_at %s,
					PRIMARY KEY (provider_id, provider_subject),
					UNIQUE (unique_id, provider_id),
					FOREIGN KEY (unique_id) REFERENCES identica_accounts(unique_id) ON DELETE CASCADE
				)
				""".formatted(uuidType, booleanType, timeType, timeType);
	}
}
