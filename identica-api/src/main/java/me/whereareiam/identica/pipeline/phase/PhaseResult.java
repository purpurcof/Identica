package me.whereareiam.identica.pipeline.phase;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class PhaseResult<S> {
	private final @NotNull S state;

	private PhaseResult(
			@NotNull S state
	) {
		this.state = state;
	}

	public static <S> @NotNull PhaseResult<S> pass(@NotNull S state) {
		return new PhaseResult<>(state);
	}
}
