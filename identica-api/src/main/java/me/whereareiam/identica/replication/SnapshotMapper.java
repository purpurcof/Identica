package me.whereareiam.identica.replication;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Converts between full models and lightweight snapshots for replication.
 *
 * @param <T> full model type
 * @param <S> snapshot type
 */
public interface SnapshotMapper<T, S> {
	/**
	 * Converts a full model into a snapshot.
	 *
	 * @param model model instance
	 * @return snapshot representation
	 */
	S toSnapshot(@NotNull T model);

	/**
	 * Hydrates a full model from a snapshot.
	 *
	 * @param snapshot snapshot representation
	 * @return future producing the hydrated model
	 */
	@NotNull CompletableFuture<T> fromSnapshot(@NotNull S snapshot);

	/**
	 * Identity mapper for snapshot types equal to the model.
	 *
	 * @param <T> model type
	 * @return identity mapper
	 */
	static <T> @NotNull SnapshotMapper<T, T> identity() {
		return new SnapshotMapper<>() {
			@Override
			public T toSnapshot(@NotNull T model) {
				return model;
			}

			@Override
			public @NotNull CompletableFuture<T> fromSnapshot(@NotNull T snapshot) {
				return CompletableFuture.completedFuture(snapshot);
			}
		};
	}
}
