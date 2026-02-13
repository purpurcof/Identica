package me.whereareiam.identica.provider.migration;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@Getter
@Builder
@ToString
public class MigrationPrecheckResult {
	private boolean allowed;
	private @Nullable String message;
	private @Nullable String kickMessage;

	public static MigrationPrecheckResult allow() {
		return MigrationPrecheckResult.builder()
				.allowed(true)
				.build();
	}

	public static MigrationPrecheckResult allow(@Nullable String kickMessage) {
		return MigrationPrecheckResult.builder()
				.allowed(true)
				.kickMessage(kickMessage)
				.build();
	}

	public static MigrationPrecheckResult deny(@Nullable String message) {
		return MigrationPrecheckResult.builder()
				.allowed(false)
				.message(message)
				.build();
	}
}
