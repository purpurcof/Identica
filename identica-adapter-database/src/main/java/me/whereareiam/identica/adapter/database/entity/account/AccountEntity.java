package me.whereareiam.identica.adapter.database.entity.account;

import lombok.*;
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
	private String username;
	private long createdAt;
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
		return """
				CREATE TABLE IF NOT EXISTS identica_accounts (
					unique_id %s PRIMARY KEY,
					username VARCHAR(64) NOT NULL,
					created_at %s,
					last_seen_at %s
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
