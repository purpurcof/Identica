package me.whereareiam.identica.model.conflict;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of conflict resolution.
 */
@Getter
@ToString
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ConflictResolution {
	private final @NotNull Decision decision;
	private final @Nullable String message;
	private final @NotNull Map<String, Object> effects;

	/**
	 * Resolution decisions.
	 */
	public enum Decision {
		ALLOW,
		PASS,
		DENY
	}

	/**
	 * Allow the candidate value as-is.
	 *
	 * @return allow resolution
	 */
	public static ConflictResolution allow() {
		return new ConflictResolution(Decision.ALLOW, null, Map.of());
	}

	/**
	 * Pass to the next resolver.
	 *
	 * @return pass resolution
	 */
	public static ConflictResolution pass() {
		return new ConflictResolution(Decision.PASS, null, Map.of());
	}

	/**
	 * Deny the operation.
	 *
	 * @param message optional message
	 * @return deny resolution
	 */
	public static ConflictResolution deny(@Nullable String message) {
		return new ConflictResolution(Decision.DENY, message, Map.of());
	}

	/**
	 * Read a typed effect value.
	 *
	 * @param key effect key
	 * @param type expected type
	 * @return typed effect or {@code null}
	 */
	public @Nullable <T> T getEffect(@NotNull String key, @NotNull Class<T> type) {
		Object value = effects.get(key);
		if (type.isInstance(value)) return type.cast(value);
		return null;
	}

	/**
	 * Returns {@code true} when this resolution contains the given effect.
	 *
	 * @param key effect key
	 * @return whether the effect is present
	 */
	public boolean hasEffect(@NotNull String key) {
		return effects.containsKey(key);
	}

	/**
	 * Return a copy of this resolution with an additional effect.
	 *
	 * @param key effect key
	 * @param value effect value
	 * @return copied resolution
	 */
	public @NotNull ConflictResolution withEffect(@NotNull String key, @Nullable Object value) {
		if (key.isBlank() || value == null) return this;

		Map<String, Object> updated = new LinkedHashMap<>(effects);
		updated.put(key, value);
		return new ConflictResolution(decision, message, Map.copyOf(updated));
	}
}
