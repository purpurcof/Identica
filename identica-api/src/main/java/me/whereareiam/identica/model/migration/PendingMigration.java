package me.whereareiam.identica.model.migration;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.migration.MigrationInitiator;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Builder
@ToString
public class PendingMigration {
	private @Nullable UUID uniqueId;
	private @Nullable UUID connectionUniqueId;
	private @Nullable String targetProviderId;
	private long requestedAt;
	private @Nullable MigrationInitiator initiator;
	private @Nullable UUID initiatorUniqueId;
	private Phase phase;

	public enum Phase {
		CONFIRMATION,
		STARTED
	}
}
