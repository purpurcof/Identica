package me.whereareiam.identica.model.conflict.participant;

import lombok.*;
import me.whereareiam.identica.model.conflict.ConflictAttributeKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic participant involved in a conflict.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class ConflictParticipant {
	/**
	 * Participant role identifier such as {@code incoming} or {@code existing}.
	 */
	private @NotNull ConflictParticipantRole role;

	/**
	 * Arbitrary participant attributes.
	 */
	@Builder.Default
	private @NotNull Map<String, Object> attributes = new HashMap<>();

	/**
	 * Read a typed participant attribute.
	 *
	 * @param key attribute key
	 * @param type expected type
	 * @return typed value or {@code null}
	 */
	public @Nullable <T> T getAttribute(@NotNull ConflictAttributeKey<T> key) {
		Object value = attributes.get(key.getId());
		if (key.getType().isInstance(value)) return key.getType().cast(value);
		return null;
	}

	/**
	 * Store a participant attribute.
	 *
	 * @param key attribute key
	 * @param value attribute value
	 */
	public <T> void putAttribute(@NotNull ConflictAttributeKey<T> key, @Nullable T value) {
		if (value == null) {
			attributes.remove(key.getId());
			return;
		}

		attributes.put(key.getId(), value);
	}

	/**
	 * Removes a participant attribute.
	 *
	 * @param key attribute key
	 * @return {@code true} when removed
	 */
	public boolean removeAttribute(@NotNull ConflictAttributeKey<?> key) {
		return attributes.remove(key.getId()) != null;
	}
}
