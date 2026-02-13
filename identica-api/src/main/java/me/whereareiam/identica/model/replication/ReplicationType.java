package me.whereareiam.identica.model.replication;

import me.whereareiam.identica.replication.codec.SnapshotCodec;
import me.whereareiam.identica.replication.SnapshotMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes how to replicate a given model type.
 *
 * @param <T> full model type
 * @param <S> snapshot type
 */
public final class ReplicationType<T, S> {
	private final @NotNull Class<S> snapshotType;
	private final @NotNull SnapshotMapper<T, S> mapper;
	private final @Nullable SnapshotCodec<S> codecOverride;
	private final int version;

	private ReplicationType(
			@NotNull Class<S> snapshotType,
			@NotNull SnapshotMapper<T, S> mapper,
			@Nullable SnapshotCodec<S> codecOverride,
			int version
	) {
		this.snapshotType = snapshotType;
		this.mapper = mapper;
		this.codecOverride = codecOverride;
		this.version = version;
	}

	/**
	 * Uses the model type as its own snapshot type.
	 *
	 * @param type model type
	 * @param <T> model type
	 * @return replication type
	 */
	public static <T> @NotNull ReplicationType<T, T> identity(@NotNull Class<T> type) {
		return new ReplicationType<>(type, SnapshotMapper.identity(), null, 1);
	}

	/**
	 * Creates a replication type using an explicit snapshot and mapper.
	 *
	 * @param snapshotType snapshot type
	 * @param mapper snapshot mapper
	 * @param <T> model type
	 * @param <S> snapshot type
	 * @return replication type
	 */
	public static <T, S> @NotNull ReplicationType<T, S> ofSnapshot(
			@NotNull Class<S> snapshotType,
			@NotNull SnapshotMapper<T, S> mapper
	) {
		return new ReplicationType<>(snapshotType, mapper, null, 1);
	}

	/**
	 * Overrides the default codec for this type.
	 *
	 * @param codec codec override
	 * @return new replication type with override
	 */
	public @NotNull ReplicationType<T, S> withCodec(@NotNull SnapshotCodec<S> codec) {
		return new ReplicationType<>(snapshotType, mapper, codec, version);
	}

	/**
	 * Snapshot type for this replication type.
	 *
	 * @return snapshot class
	 */
	public @NotNull Class<S> snapshotType() {
		return snapshotType;
	}

	/**
	 * Snapshot mapper for this replication type.
	 *
	 * @return mapper
	 */
	public @NotNull SnapshotMapper<T, S> mapper() {
		return mapper;
	}

	/**
	 * Optional codec override.
	 *
	 * @return codec override or null
	 */
	public @Nullable SnapshotCodec<S> codecOverride() {
		return codecOverride;
	}

	/**
	 * Envelope version for this type.
	 *
	 * @return version number
	 */
	public int version() {
		return version;
	}
}
