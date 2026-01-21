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
@Entity(tableName = "identica_accounts")
public class AccountEntity implements EntitySchemaProvider {
	private UUID uniqueId;
	private long createdAt;
	private long lastSeenAt;

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
				CREATE TABLE IF NOT EXISTS identica_accounts (
					uuid %s PRIMARY KEY,
					created_at %s,
					last_seen_at %s
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
