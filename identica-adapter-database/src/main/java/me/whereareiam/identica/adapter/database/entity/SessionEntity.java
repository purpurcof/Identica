package me.whereareiam.identica.adapter.database.entity;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.dialectica.EntitySchemaProvider;
import me.whereareiam.dialectica.annotation.Entity;
import me.whereareiam.dialectica.type.DatabaseType;

@Getter
@Setter
@Entity(tableName = "identica_sessions")
public class SessionEntity implements EntitySchemaProvider {
	private String sessionId;
	private String uuid;
	private long expiresAt;
	private String ip;
	private long createdAt;

	@Override
	public String statement(String databaseType) {
		String uuidType = DatabaseType.POSTGRES.equals(databaseType) ? "UUID"
				: DatabaseType.SQLITE.equals(databaseType) ? "TEXT" : "CHAR(36)";
		String timeType = DatabaseType.SQLITE.equals(databaseType) ? "INTEGER" : "BIGINT";
		return """
				CREATE TABLE IF NOT EXISTS identica_sessions (
					session_id VARCHAR(128) PRIMARY KEY,
					uuid %s NOT NULL,
					expires_at %s,
					ip VARCHAR(64),
					created_at %s
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
