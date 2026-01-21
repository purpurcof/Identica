package me.whereareiam.identica.adapter.database.entity;

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
@Entity(tableName = "identica_account_links")
public class AccountLinkEntity implements EntitySchemaProvider {
	private UUID uniqueId;
	private String providerId;
	private boolean primary;
	private long linkedAt;
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

		String booleanType = switch (type) {
			case "SQLITE" -> "INTEGER";
			default -> "BOOLEAN";
		};

		return """
				CREATE TABLE IF NOT EXISTS identica_account_links (
					unique_id %s NOT NULL,
					provider_id VARCHAR(64) NOT NULL,
					is_primary %s NOT NULL,
					linked_at %s,
					last_used_at %s,
					PRIMARY KEY (unique_id, provider_id)
				)
				""".formatted(uuidType, booleanType, timeType, timeType);
	}
}
