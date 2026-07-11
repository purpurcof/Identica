package me.whereareiam.identica.model.conflict;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Typed key used for conflict context and participant attributes.
 *
 * @param <T> value type
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ConflictAttributeKey<T> {
	private final @NotNull String id;
	private final @NotNull Class<T> type;

	/**
	 * Creates a new typed conflict attribute key.
	 *
	 * @param id stable attribute identifier
	 * @param type attribute type
	 */
	public ConflictAttributeKey(@NotNull String id, @NotNull Class<T> type) {
		if (id.isBlank())
			throw new IllegalArgumentException("attribute id cannot be blank");

		this.id = id;
		this.type = type;
	}

	/**
	 * Creates a typed key for arbitrary object values.
	 *
	 * @param id stable attribute identifier
	 * @param type attribute type
	 * @param <T> value type
	 * @return typed key
	 */
	public static @NotNull <T> ConflictAttributeKey<T> of(
			@NotNull String id,
			@NotNull Class<T> type
	) {
		return new ConflictAttributeKey<>(id, type);
	}

	/**
	 * Creates a typed key for string values.
	 *
	 * @param id stable attribute identifier
	 * @return typed key
	 */
	public static @NotNull ConflictAttributeKey<String> string(@NotNull String id) {
		return of(id, String.class);
	}

	/**
	 * Creates a typed key for boolean values.
	 *
	 * @param id stable attribute identifier
	 * @return typed key
	 */
	public static @NotNull ConflictAttributeKey<Boolean> bool(@NotNull String id) {
		return of(id, Boolean.class);
	}
}
