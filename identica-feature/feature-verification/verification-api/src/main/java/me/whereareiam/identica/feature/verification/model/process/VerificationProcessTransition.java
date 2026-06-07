package me.whereareiam.identica.feature.verification.model.process;

import lombok.*;
import me.whereareiam.identica.feature.verification.type.process.VerificationProcessTransitionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Transition instruction returned by a verification process step.
 *
 * <p>The owning process resolves the transition and persists the next cursor
 * when the process remains active.</p>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationProcessTransition {
	private @NotNull VerificationProcessTransitionType type;
	private @Nullable String targetStepId;

	/**
	 * Returns a transition that keeps the cursor on the current step.
	 *
	 * @return stay transition
	 */
	public static @NotNull VerificationProcessTransition stay() {
		return VerificationProcessTransition.builder()
				.type(VerificationProcessTransitionType.STAY)
				.build();
	}

	/**
	 * Returns a transition that advances to the next process-defined step.
	 *
	 * @return advance transition
	 */
	public static @NotNull VerificationProcessTransition advance() {
		return VerificationProcessTransition.builder()
				.type(VerificationProcessTransitionType.ADVANCE)
				.build();
	}

	/**
	 * Returns a transition that jumps to a specific step id.
	 *
	 * @param targetStepId stable destination step id
	 * @return goto transition
	 */
	public static @NotNull VerificationProcessTransition goTo(@NotNull String targetStepId) {
		return VerificationProcessTransition.builder()
				.type(VerificationProcessTransitionType.GOTO)
				.targetStepId(targetStepId)
				.build();
	}

	/**
	 * Returns a transition that completes the current process.
	 *
	 * @return complete transition
	 */
	public static @NotNull VerificationProcessTransition complete() {
		return VerificationProcessTransition.builder()
				.type(VerificationProcessTransitionType.COMPLETE)
				.build();
	}
}
