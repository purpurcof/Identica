package me.whereareiam.identica.model.connection;

import lombok.Getter;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.stage.PendingStage;
import me.whereareiam.identica.stage.StepStage;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Snapshot of a paused authentication flow for a connection.
 */
@Getter
public class FlowState {
	private final @NotNull AuthFlowType flow;
	private final @NotNull List<StepStage> stages;
	private final int stageIndex;
	private final @NotNull PendingStage pendingStage;
	private final @NotNull AuthContext context;
	private final @Nullable StepResult completionResult;

	/**
	 * Creates a flow state snapshot for a paused authentication pipeline.
	 *
	 * @param flow flow type
	 * @param stages resolved stages
	 * @param stageIndex index of the current stage
	 * @param pendingStage pending stage data
	 * @param context authentication context
	 * @param completionResult completion result, if available
	 */
	public FlowState(
			@NotNull AuthFlowType flow,
			@NotNull List<StepStage> stages,
			int stageIndex,
			@NotNull PendingStage pendingStage,
			@NotNull AuthContext context,
			@Nullable StepResult completionResult
	) {
		this.flow = flow;
		this.stages = stages;
		this.stageIndex = stageIndex;
		this.pendingStage = pendingStage;
		this.context = context;
		this.completionResult = completionResult;
	}
}
