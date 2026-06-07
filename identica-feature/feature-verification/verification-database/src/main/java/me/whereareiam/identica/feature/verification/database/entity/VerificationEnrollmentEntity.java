package me.whereareiam.identica.feature.verification.database.entity;

import lombok.*;
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
	private String enrollmentId;
	private String credential;
	private String label;
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
		String credentialType = switch (type) {
			case "POSTGRES" -> "TEXT";
			case "H2" -> "CLOB";
			default -> "TEXT";
		};

		return """
				CREATE TABLE IF NOT EXISTS identica_verification_enrollments (
					unique_id %s NOT NULL,
					method_id VARCHAR(64) NOT NULL,
					enrollment_id VARCHAR(64) NOT NULL,
					credential %s NOT NULL,
					label VARCHAR(64),
					created_at %s,
					enabled_at %s,
					PRIMARY KEY (unique_id, method_id, enrollment_id),
					FOREIGN KEY (unique_id) REFERENCES identica_accounts(unique_id) ON DELETE CASCADE
				)
				""".formatted(uuidType, credentialType, timeType, timeType);
	}
}
