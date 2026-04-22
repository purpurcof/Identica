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

	public static <S extends VerificationProcessState> @NotNull VerificationProcessResult<S> waiting(@Nullable S state) {
		return VerificationProcessResult.<S>builder()
				.status(VerificationProcessStatus.WAITING)
				.state(state)
				.build();
	}

	public static <S extends VerificationProcessState> @NotNull VerificationProcessResult<S> verified(@Nullable S state) {
		return VerificationProcessResult.<S>builder()
				.status(VerificationProcessStatus.VERIFIED)
				.state(state)
				.build();
	}
}
