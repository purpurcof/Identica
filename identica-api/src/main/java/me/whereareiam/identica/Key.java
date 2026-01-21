package me.whereareiam.identica;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Typesafe key for storing and retrieving data in contexts.
 *
 * @param <T> the type of value this key represents
 */
@Getter
@RequiredArgsConstructor
@SuppressWarnings("unused")
public final class Key<T> {
	private final String name;
	private final Class<T> type;

	/**
	 * Create a new typesafe key.
	 */
	public static <T> Key<T> create(String name, Class<T> type) {
		return new Key<>(name, type);
	}

	public T cast(Object value) {
		return type.cast(value);
	}
}
