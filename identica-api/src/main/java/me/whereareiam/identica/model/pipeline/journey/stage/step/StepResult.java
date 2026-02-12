package me.whereareiam.identica.model.pipeline.journey.stage.step;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.pipeline.ScenarioContext;
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

	public static @NotNull StepResult proceed(@Nullable ScenarioContext context) {
		return new StepResult(StepStatus.CONTINUE, null, context);
	}

	public static @NotNull StepResult waiting(@Nullable String message) {
		return new StepResult(StepStatus.WAITING, message, null);
	}

	public static @NotNull StepResult complete(@Nullable ScenarioContext context) {
		return new StepResult(StepStatus.COMPLETE, null, context);
	}

	public static @NotNull StepResult failed(@Nullable String message) {
		return new StepResult(StepStatus.FAILED, message, null);
	}

	public static @NotNull StepResult denied(@Nullable String message) {
		return new StepResult(StepStatus.DENIED, message, null);
	}

	public static @NotNull StepResult requireReconnect(@Nullable String message) {
		return new StepResult(StepStatus.REQUIRE_RECONNECT, message, null);
	}

	public static @NotNull StepResult noPending() {
		return new StepResult(StepStatus.NO_PENDING, null, null);
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
