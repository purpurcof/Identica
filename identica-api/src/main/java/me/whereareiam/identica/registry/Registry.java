package me.whereareiam.identica.registry;

import java.util.Set;

@SuppressWarnings("unused")
public interface Registry<T> {
	void register(T value);

	void unregister(T value);

	Set<T> values();
}
