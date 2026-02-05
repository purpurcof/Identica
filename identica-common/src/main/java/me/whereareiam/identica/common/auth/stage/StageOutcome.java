package me.whereareiam.identica.common.auth.stage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.auth.stage.PendingStage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class StageOutcome {
	private final @NotNull StepResult result;
	private final @NotNull AuthContext context;
	private final boolean advanceStage;
	private final @Nullable StepResult completionResult;
	private final @Nullable PendingStage pendingStage;

	public static @NotNull StageOutcome advance(
			@NotNull StepResult result,
			@NotNull AuthContext context,
			@Nullable StepResult completionResult
	) {
		return new StageOutcome(result, context, true, completionResult, null);
	}

	public static @NotNull StageOutcome result(
			@NotNull StepResult result,
			@NotNull AuthContext context,
			@Nullable StepResult completionResult
	) {
		return new StageOutcome(result, context, false, completionResult, null);
	}

	public static @NotNull StageOutcome waiting(
			@NotNull StepResult result,
			@NotNull AuthContext context,
			@Nullable StepResult completionResult,
			@NotNull PendingStage pendingStage
	) {
		return new StageOutcome(result, context, false, completionResult, pendingStage);
	}
}
