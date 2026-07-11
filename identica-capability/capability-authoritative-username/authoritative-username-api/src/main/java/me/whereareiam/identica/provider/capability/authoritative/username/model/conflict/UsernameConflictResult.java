package me.whereareiam.identica.provider.capability.authoritative.username.model.conflict;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public class UsernameConflictResult {
	private final boolean denied;
	private final @Nullable String denialMessage;
	private final @NotNull String effectiveUsername;

	public static @NotNull UsernameConflictResult allowed(@NotNull String effectiveUsername) {
		return new UsernameConflictResult(false, null, effectiveUsername);
	}

	public static @NotNull UsernameConflictResult denied(
			@Nullable String denialMessage,
			@NotNull String effectiveUsername
	) {
		return new UsernameConflictResult(true, denialMessage, effectiveUsername);
	}
}
