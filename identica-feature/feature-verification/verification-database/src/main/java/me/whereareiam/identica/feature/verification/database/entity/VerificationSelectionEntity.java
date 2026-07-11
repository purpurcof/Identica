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
@Entity(tableName = "identica_verification_selections")
public class VerificationSelectionEntity implements EntitySchemaProvider {
	private UUID uniqueId;
	private String providerId;
	private String methodId;
	private long selectedAt;

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
				CREATE TABLE IF NOT EXISTS identica_verification_selections (
					unique_id %s NOT NULL,
					provider_id VARCHAR(64) NOT NULL,
					method_id VARCHAR(64) NOT NULL,
					selected_at %s,
					PRIMARY KEY (unique_id, provider_id),
					FOREIGN KEY (unique_id) REFERENCES identica_accounts(unique_id) ON DELETE CASCADE
				)
				""".formatted(uuidType, timeType);
	}
}
