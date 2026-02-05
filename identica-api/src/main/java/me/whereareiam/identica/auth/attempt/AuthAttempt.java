package me.whereareiam.identica.auth.attempt;

import lombok.Getter;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.auth.stage.PendingStage;
import me.whereareiam.identica.auth.stage.StepStage;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Snapshot of a pending authentication attempt.
 */
@SuppressWarnings("unused")
public class AuthAttempt {
	private final @NotNull UUID attemptId;
	private final @NotNull AuthFlowType flow;
	private final @NotNull List<StepStage> stages;
	@Getter
	private final int stageIndex;
	private final @NotNull PendingStage pendingStage;
	private final @NotNull AuthContext context;
	private final @Nullable StepResult completionResult;
	@Getter
	private final long createdAt;

	/**
	 * Creates a pending authentication attempt snapshot.
	 *
	 * @param attemptId attempt unique id
	 * @param flow flow type
	 * @param stages resolved stages
	 * @param stageIndex index of the current stage
	 * @param pendingStage pending stage data
	 * @param context authentication context
	 * @param completionResult completion result, if available
	 * @param createdAt creation timestamp in millis
	 */
	public AuthAttempt(
			@NotNull UUID attemptId,
			@NotNull AuthFlowType flow,
			@NotNull List<StepStage> stages,
			int stageIndex,
			@NotNull PendingStage pendingStage,
			@NotNull AuthContext context,
			@Nullable StepResult completionResult,
			long createdAt
	) {
		this.attemptId = attemptId;
		this.flow = flow;
		this.stages = stages;
		this.stageIndex = stageIndex;
		this.pendingStage = pendingStage;
		this.context = context;
		this.completionResult = completionResult;
		this.createdAt = createdAt;
	}

	/**
	 * Returns the attempt unique id.
	 *
	 * @return attempt id
	 */
	public @NotNull UUID getAttemptId() {
		return attemptId;
	}

	/**
	 * Returns the flow type for this attempt.
	 *
	 * @return flow type
	 */
	public @NotNull AuthFlowType getFlow() {
		return flow;
	}

	/**
	 * Returns the resolved stages for this attempt.
	 *
	 * @return stages
	 */
	public @NotNull List<StepStage> getStages() {
		return stages;
	}

	/**
	 * Returns the pending stage snapshot.
	 *
	 * @return pending stage
	 */
	public @NotNull PendingStage getPendingStage() {
		return pendingStage;
	}

	/**
	 * Returns the authentication context for this attempt.
	 *
	 * @return authentication context
	 */
	public @NotNull AuthContext getContext() {
		return context;
	}

	/**
	 * Returns the completion result, if available.
	 *
	 * @return completion result or {@code null}
	 */
	public @Nullable StepResult getCompletionResult() {
		return completionResult;
	}
}
