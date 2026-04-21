package me.whereareiam.identica.model.pipeline.journey.stage.step;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.pipeline.journey.step.StepWaitReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class StepResult {
	private final @NotNull StepStatus status;
	private final @Nullable String message;
	private final @Nullable ScenarioContext updatedContext;
	private final @Nullable StepWaitReason waitReason;

	public static @NotNull StepResult proceed(@Nullable ScenarioContext context) {
		return new StepResult(StepStatus.CONTINUE, null, context, null);
	}

	public static @NotNull StepResult waiting(@Nullable String message) {
		return waiting(message, StepWaitReason.INPUT);
	}

	public static @NotNull StepResult waiting(@Nullable String message, @NotNull StepWaitReason waitReason) {
		return new StepResult(StepStatus.WAITING, message, null, waitReason);
	}

	public static @NotNull StepResult complete(@Nullable ScenarioContext context) {
		return new StepResult(StepStatus.COMPLETE, null, context, null);
	}

	public static @NotNull StepResult failed(@Nullable String message) {
		return new StepResult(StepStatus.FAILED, message, null, null);
	}

	public static @NotNull StepResult denied(@Nullable String message) {
		return new StepResult(StepStatus.DENIED, message, null, null);
	}

	public static @NotNull StepResult requireReconnect(@Nullable String message) {
		return new StepResult(StepStatus.REQUIRE_RECONNECT, message, null, null);
	}

	public static @NotNull StepResult noPending() {
		return new StepResult(StepStatus.NO_PENDING, null, null, null);
	}

	public enum StepStatus {
		CONTINUE,
		WAITING,
		COMPLETE,
		FAILED,
		DENIED,
		REQUIRE_RECONNECT,
		NO_PENDING
	}
}
