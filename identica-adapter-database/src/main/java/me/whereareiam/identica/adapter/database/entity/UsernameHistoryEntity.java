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
@Entity(tableName = "identica_username_history")
public class UsernameHistoryEntity implements EntitySchemaProvider {
	private UUID uniqueId;
	private String providerId;
	private String oldUsername;
	private String newUsername;
	private String source;
	private long changedAt;

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
				CREATE TABLE IF NOT EXISTS identica_username_history (
					unique_id %s NOT NULL,
					provider_id VARCHAR(64),
					old_username VARCHAR(64) NOT NULL,
					new_username VARCHAR(64) NOT NULL,
					source VARCHAR(32) NOT NULL,
					changed_at %s NOT NULL
				)
				""".formatted(uuidType, timeType);
	}
}
