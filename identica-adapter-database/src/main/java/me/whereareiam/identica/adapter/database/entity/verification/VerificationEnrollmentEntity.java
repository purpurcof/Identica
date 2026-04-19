package me.whereareiam.identica.adapter.database.entity.verification;

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
@Entity(tableName = "identica_verification_enrollments")
public class VerificationEnrollmentEntity implements EntitySchemaProvider {
	private UUID uniqueId;
	private String methodId;
	private String payload;
	private long createdAt;
	private long enabledAt;

	@Override
	public String statement(String databaseType) {
		String type = databaseType == null ? "" : databaseType.toUpperCase();
		String uuidType = switch (type) {
			case "POSTGRES", "H2" -> "UUID";
			case "SQLITE" -> "TEXT";
			default -> "CHAR(36)";
		};
		String timeType = "SQLITE".equals(type) ? "INTEGER" : "BIGINT";

		return """
				CREATE TABLE IF NOT EXISTS identica_verification_enrollments (
					unique_id %s NOT NULL,
					method_id VARCHAR(64) NOT NULL,
					payload VARCHAR(1024) NOT NULL,
					created_at %s,
					enabled_at %s,
					PRIMARY KEY (unique_id, method_id),
					FOREIGN KEY (unique_id) REFERENCES identica_accounts(unique_id) ON DELETE CASCADE
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
