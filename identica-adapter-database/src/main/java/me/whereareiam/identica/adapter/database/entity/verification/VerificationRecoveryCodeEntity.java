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
@Entity(tableName = "identica_verification_recovery_codes")
public class VerificationRecoveryCodeEntity implements EntitySchemaProvider {
	private UUID uniqueId;
	private String methodId;
	private String codeHash;
	private long createdAt;
	private long usedAt;

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
				CREATE TABLE IF NOT EXISTS identica_verification_recovery_codes (
					unique_id %s NOT NULL,
					method_id VARCHAR(64) NOT NULL,
					code_hash VARCHAR(128) NOT NULL,
					created_at %s,
					used_at %s,
					PRIMARY KEY (unique_id, method_id, code_hash),
					FOREIGN KEY (unique_id, method_id)
						REFERENCES identica_verification_enrollments(unique_id, method_id)
						ON DELETE CASCADE
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
