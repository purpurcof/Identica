package me.whereareiam.identica.model.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Result of executing an authentication step.
 */
@Getter
@RequiredArgsConstructor
public class StepResult {
	private final StepStatus status;
	private final String message;
	private final AuthContext updatedContext;

	public static StepResult proceed(AuthContext context) {
		return new StepResult(StepStatus.CONTINUE, null, context);
	}

	public static StepResult waiting(String message) {
		return new StepResult(StepStatus.WAITING, message, null);
	}

	public static StepResult complete(AuthContext context) {
		return new StepResult(StepStatus.COMPLETE, null, context);
	}

	public static StepResult failed(String message) {
		return new StepResult(StepStatus.FAILED, message, null);
	}

	public enum StepStatus {
		CONTINUE,
		WAITING,
		COMPLETE,
		FAILED
	}
}
