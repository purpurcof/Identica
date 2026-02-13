package me.whereareiam.identica.provider.migration;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.migration.MigrationInitiator;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Builder
@ToString
public class MigrationPrecheckContext {
	private @Nullable UUID connectionUniqueId;
	private @Nullable UUID accountUniqueId;
	private @Nullable String username;
	private @Nullable String ip;
	private @Nullable String providerId;
	private @Nullable MigrationInitiator initiator;
	private @Nullable UUID initiatorUniqueId;
}
