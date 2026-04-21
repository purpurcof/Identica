package me.whereareiam.identica.adapter.database.entity.account;

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
@Entity(tableName = "identica_account_reservations")
public class AccountReservationEntity implements EntitySchemaProvider {
	private String reservationKey;
	private UUID uniqueId;
	private long createdAt;
	private long expiresAt;

	@Override
	public String statement(String databaseType) {
		String type = databaseType == null ? "" : databaseType.toUpperCase();
		String uuidType = switch (type) {
			case "POSTGRES", "H2" -> "UUID";
			case "SQLITE" -> "TEXT";
			default -> "CHAR(36)";
		};
		String timeType = switch (type) {
			case "SQLITE" -> "INTEGER";
			default -> "BIGINT";
		};
		return """
				CREATE TABLE IF NOT EXISTS identica_account_reservations (
					reservation_key VARCHAR(192) PRIMARY KEY,
					unique_id %s NOT NULL,
					created_at %s NOT NULL,
					expires_at %s NOT NULL
				)
				""".formatted(uuidType, timeType, timeType);
	}
}
