package me.whereareiam.identica.model.migration.operation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Builder
@ToString
public class MigrationConfirm {
	private @Nullable UUID connectionUniqueId;
	private @Nullable String kickMessage;
}
