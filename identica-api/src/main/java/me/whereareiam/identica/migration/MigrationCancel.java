package me.whereareiam.identica.migration;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.migration.MigrationCancelScope;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Builder
@ToString
public class MigrationCancel {
	private @Nullable UUID connectionUniqueId;
	private @Nullable MigrationCancelScope scope;
}
