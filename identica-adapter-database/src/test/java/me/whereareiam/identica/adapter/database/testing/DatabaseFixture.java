package me.whereareiam.identica.adapter.database.testing;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DatabaseFixture {
	String name();

	Jdbi jdbi();

	default void prepareHandle(Handle handle) {
	}

	default void useHandle(Consumer<Handle> consumer) {
		jdbi().useHandle(handle -> {
			prepareHandle(handle);
			consumer.accept(handle);
		});
	}

	default <T> T withHandle(Function<Handle, T> function) {
		return jdbi().withHandle(handle -> {
			prepareHandle(handle);
			return function.apply(handle);
		});
	}

	void reset();

	void shutdown();
}
