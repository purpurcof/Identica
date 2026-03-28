package me.whereareiam.identica.model.migration.operation;

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
