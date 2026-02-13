package me.whereareiam.identica.pipeline.phase;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public final class PhasePlacement {
	private final @NotNull Type type;
	private final @Nullable String anchorPhaseId;

	private PhasePlacement(@NotNull Type type, @Nullable String anchorPhaseId) {
		this.type = type;
		this.anchorPhaseId = anchorPhaseId;
	}

	public static @NotNull PhasePlacement first() {
		return new PhasePlacement(Type.FIRST, null);
	}

	public static @NotNull PhasePlacement last() {
		return new PhasePlacement(Type.LAST, null);
	}

	public static @NotNull PhasePlacement before(@NotNull String anchorPhaseId) {
		if (anchorPhaseId.isBlank())
			throw new IllegalArgumentException("anchorPhaseId cannot be blank");
		return new PhasePlacement(Type.BEFORE, anchorPhaseId);
	}

	public static @NotNull PhasePlacement after(@NotNull String anchorPhaseId) {
		if (anchorPhaseId.isBlank())
			throw new IllegalArgumentException("anchorPhaseId cannot be blank");
		return new PhasePlacement(Type.AFTER, anchorPhaseId);
	}

	public enum Type {
		FIRST,
		LAST,
		BEFORE,
		AFTER
	}
}
