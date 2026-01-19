package me.whereareiam.identica.adapter.database.entity;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.type.DatabaseType;

@Getter
@Setter
@Entity(tableName = "identica_accounts")
public class AccountEntity implements EntitySchemaProvider {
	private String uuid;
	private long createdAt;
	private long lastSeenAt;

	@Override
	public String statement(String databaseType) {
		String uuidType = DatabaseType.POSTGRES.equals(databaseType) ? "UUID"
				: DatabaseType.SQLITE.equals(databaseType) ? "TEXT" : "CHAR(36)";
		String timeType = DatabaseType.SQLITE.equals(databaseType) ? "INTEGER" : "BIGINT";
		return """
				CREATE TABLE IF NOT EXISTS identica_accounts (
					uuid %s PRIMARY KEY,
					created_at %s,
					last_seen_at %s
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
