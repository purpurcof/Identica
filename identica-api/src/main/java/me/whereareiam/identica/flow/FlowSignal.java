package me.whereareiam.identica.flow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.cache.codec.CacheCodec;
import org.jetbrains.annotations.NotNull;

/**
 * Typed flow signal descriptor used by {@link FlowTransit}.
 *
 * @param <T> signal value type
 */
@Getter
@RequiredArgsConstructor
@SuppressWarnings("unused")
public final class FlowSignal<T> {
	private final @NotNull String key;
	private final @NotNull CacheCodec<T> codec;

	public static <T> @NotNull FlowSignal<T> of(
			@NotNull String key,
			@NotNull CacheCodec<T> codec
	) {
		return new FlowSignal<>(key, codec);
	}
}
