package me.whereareiam.identica.auth.stage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Captures the pending steps for a stage that has paused execution.
 */
@Getter
@RequiredArgsConstructor
public final class PendingStage {
	private final @Nullable String providerId;
	private final @NotNull List<AuthenticationStep> steps;
	private final int stepIndex;
}
