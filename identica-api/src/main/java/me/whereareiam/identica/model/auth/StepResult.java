package me.whereareiam.identica.model.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.type.HandshakeMode;

/**
 * Result of executing an authentication step.
 */
@Getter
@RequiredArgsConstructor
public class StepResult {
	private final StepStatus status;
	private final String message;
	private final AuthContext updatedContext;
	private final HandshakeMode handshakeMode;

	public static StepResult proceed(AuthContext context) {
		return new StepResult(StepStatus.CONTINUE, null, context, null);
	}

	public static StepResult waiting(String message) {
		return new StepResult(StepStatus.WAITING, message, null, null);
	}

	public static StepResult complete(AuthContext context) {
		return new StepResult(StepStatus.COMPLETE, null, context, null);
	}

	public static StepResult failed(String message) {
		return new StepResult(StepStatus.FAILED, message, null, null);
	}

	public static StepResult denied(String message) {
		return new StepResult(StepStatus.DENIED, message, null, null);
	}

	public static StepResult requireReconnect(HandshakeMode mode, String message) {
		return new StepResult(StepStatus.REQUIRE_RECONNECT, message, null, mode);
	}

	public static StepResult noPending() {
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
