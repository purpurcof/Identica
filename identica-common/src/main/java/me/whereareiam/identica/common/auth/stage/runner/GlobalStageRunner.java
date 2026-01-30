package me.whereareiam.identica.common.auth.stage.runner;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.common.auth.stage.StageOutcome;
import me.whereareiam.identica.common.auth.step.StepExecutor;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.stage.PendingStage;
import me.whereareiam.identica.stage.StepStage;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GlobalStageRunner {
	private final StepExecutor stepExecutor;
	private final Provider<Messages> messagesProvider;

	@NotNull
	public CompletionStage<StageOutcome> run(
			@NotNull StepStage stage,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult
	) {
		return execute(stage, context, flow, completionResult, null, 0);
	}

	@NotNull
	public CompletionStage<StageOutcome> resume(
			@NotNull StepStage stage,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult,
			@NotNull PendingStage pendingStage
	) {
		return execute(stage, context, flow, completionResult, pendingStage.getSteps(), pendingStage.getStepIndex() + 1);
	}

	private CompletionStage<StageOutcome> execute(
			@NotNull StepStage stage,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult,
			@Nullable List<AuthenticationStep> stepsOverride,
			int stepIndex
	) {
		List<AuthenticationStep> steps = stepsOverride != null
				? stepsOverride
				: stage.steps(context, flow, null);

		return stepExecutor.execute(null, stage.phase(), steps, context, stepIndex, stage.requireCompletion())
				.thenApply(result -> handleStageResult(stage, flow, completionResult, result));
	}

	private @NotNull StageOutcome handleStageResult(
			@NotNull StepStage stage,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult,
			@NotNull StepExecutor.StepExecution result
	) {
		StepResult stepResult = result.getResult();
		AuthContext next = result.getContext();

		if (stepResult.getStatus() == StepResult.StepStatus.WAITING) {
			if (flow == AuthFlowType.SEAMLESS) {
				return StageOutcome.result(StepResult.failed(
						joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed())
				), next, completionResult);
			}

			PendingStage pendingStage = new PendingStage(null, result.getSteps(), result.getStepIndex());
			return StageOutcome.waiting(stepResult, next, completionResult, pendingStage);
		}

		if (stepResult.getStatus() == StepResult.StepStatus.CONTINUE && result.isStageComplete())
			return StageOutcome.advance(stepResult, next, completionResult);

		if (stepResult.getStatus() == StepResult.StepStatus.COMPLETE) {
			if (stage.usesCompletionResult())
				return StageOutcome.advance(stepResult, next, stepResult);

			return StageOutcome.result(stepResult, next, completionResult);
		}

		return StageOutcome.result(stepResult, next, completionResult);
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
