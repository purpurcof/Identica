package me.whereareiam.identica.model.verification.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.VerificationProcessStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic typed verification process result.
 *
 * @param <S> state type
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationProcessResult<S extends VerificationProcessState> {
	private VerificationProcessStatus status;
	private @Nullable S state;
	private @Nullable VerificationProcessDisplay display;
	private @Nullable VerificationProcessTransition transition;

	/**
	 * Builds a waiting result that keeps the process on the current step.
	 *
	 * @param state state to persist
	 * @param <S> state type
	 * @return waiting result
	 */
	public static <S extends VerificationProcessState> @NotNull VerificationProcessResult<S> waiting(@Nullable S state) {
		return waiting(state, VerificationProcessTransition.stay());
	}

	/**
	 * Builds a waiting result with an explicit transition.
	 *
	 * @param state state to persist
	 * @param transition next transition to apply
	 * @param <S> state type
	 * @return waiting result
	 */
	public static <S extends VerificationProcessState> @NotNull VerificationProcessResult<S> waiting(
			@Nullable S state,
			@Nullable VerificationProcessTransition transition
	) {
		return VerificationProcessResult.<S>builder()
				.status(VerificationProcessStatus.WAITING)
				.state(state)
				.transition(transition)
				.build();
	}

	/**
	 * Builds a verified result that completes the current process.
	 *
	 * @param state final process state
	 * @param <S> state type
	 * @return verified result
	 */
	public static <S extends VerificationProcessState> @NotNull VerificationProcessResult<S> verified(@Nullable S state) {
		return verified(state, VerificationProcessTransition.complete());
	}

	/**
	 * Builds a verified result with an explicit transition.
	 *
	 * @param state final process state
	 * @param transition next transition to apply
	 * @param <S> state type
	 * @return verified result
	 */
	public static <S extends VerificationProcessState> @NotNull VerificationProcessResult<S> verified(
			@Nullable S state,
			@Nullable VerificationProcessTransition transition
	) {
		return VerificationProcessResult.<S>builder()
				.status(VerificationProcessStatus.VERIFIED)
				.state(state)
				.transition(transition)
				.build();
	}
}
