package me.whereareiam.identica.handshake;

import me.whereareiam.identica.replication.codec.SnapshotCodec;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

/**
 * Type-safe key for handshake instruction attributes.
 */
@SuppressWarnings("unused")
public final class HandshakeAttributeKey<T> {
	private final @NotNull String id;
	private final @NotNull Function<T, String> encoder;
	private final @NotNull Function<String, T> decoder;

	private HandshakeAttributeKey(
			@NotNull String id,
			@NotNull Function<T, String> encoder,
			@NotNull Function<String, T> decoder
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.encoder = Objects.requireNonNull(encoder, "encoder");
		this.decoder = Objects.requireNonNull(decoder, "decoder");
	}

	public @NotNull String id() {
		return id;
	}

	public @NotNull Function<T, String> encoder() {
		return encoder;
	}

	public @NotNull Function<String, T> decoder() {
		return decoder;
	}

	public static @NotNull HandshakeAttributeKey<String> string(@NotNull String id) {
		return of(id, value -> value, value -> value);
	}

	public static @NotNull HandshakeAttributeKey<Boolean> bool(@NotNull String id) {
		return of(id, String::valueOf, Boolean::parseBoolean);
	}

	public static @NotNull <T> HandshakeAttributeKey<T> json(
			@NotNull String id,
			@NotNull Class<T> type
	) {
		SnapshotCodec<T> codec = SnapshotCodec.json(type);
		return of(id,
				value -> new String(codec.encode(value), StandardCharsets.UTF_8),
				payload -> codec.decode(payload.getBytes(StandardCharsets.UTF_8))
		);
	}

	public static @NotNull <T> HandshakeAttributeKey<T> of(
			@NotNull String id,
			@NotNull Function<T, String> encoder,
			@NotNull Function<String, T> decoder
	) {
		return new HandshakeAttributeKey<>(id, encoder, decoder);
	}
}
