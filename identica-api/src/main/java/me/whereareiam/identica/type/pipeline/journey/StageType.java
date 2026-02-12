package me.whereareiam.identica.type.pipeline.journey;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

/**
 * Journey stage type descriptor.
 */
public final class StageType {
	public static final StageType PRE = StageType.of("pre", 100);
	public static final StageType PROVIDER = StageType.of("provider", 200);
	public static final StageType END = StageType.of("end", 300);

	private final @NotNull String id;
	private final int order;

	private StageType(@NotNull String id, int order) {
		this.id = id;
		this.order = order;
	}

	public static @NotNull StageType of(@NotNull String id, int order) {
		String normalized = normalize(id);
		if (normalized.isBlank())
			throw new IllegalArgumentException("stage type id cannot be blank");
		return new StageType(normalized, order);
	}

	public @NotNull String id() {
		return id;
	}

	public int order() {
		return order;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof StageType stageType))
			return false;
		return id.equals(stageType.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return id;
	}

	private static @NotNull String normalize(@NotNull String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
