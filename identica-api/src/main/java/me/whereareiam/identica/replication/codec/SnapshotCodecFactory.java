package me.whereareiam.identica.replication.codec;

import org.jetbrains.annotations.NotNull;

/**
 * Factory for resolving snapshot codecs.
 */
public interface SnapshotCodecFactory {
	/**
	 * Resolves a codec for the given snapshot type.
	 *
	 * @param snapshotType snapshot class
	 * @param <S> snapshot type
	 * @return codec instance
	 */
	@NotNull <S> SnapshotCodec<S> codecFor(@NotNull Class<S> snapshotType);
}
