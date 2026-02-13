package me.whereareiam.identica.model.replication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Wire envelope for replicated payloads.
 */
@Getter
@AllArgsConstructor
public class ReplicationEnvelope {
	private static final int HEADER_BYTES = Integer.BYTES + Long.BYTES;

	private final int version;
	private final long expiresAt;
	private final byte @NotNull [] payload;

	/**
	 * Encodes an envelope into bytes.
	 *
	 * @param version envelope version
	 * @param expiresAt expiration timestamp
	 * @param payload payload bytes
	 * @return encoded envelope
	 */
	public static byte @NotNull [] encode(int version, long expiresAt, byte @NotNull [] payload) {
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES + payload.length);
		buffer.putInt(version);
		buffer.putLong(expiresAt);
		buffer.put(payload);

		return buffer.array();
	}

	/**
	 * Decodes an envelope from bytes.
	 *
	 * @param data encoded envelope
	 * @return decoded envelope or null when invalid
	 */
	public static @Nullable ReplicationEnvelope decode(byte @Nullable [] data) {
		if (data == null || data.length < HEADER_BYTES) return null;

		ByteBuffer buffer = ByteBuffer.wrap(data);
		int version = buffer.getInt();
		long expiresAt = buffer.getLong();

		byte[] payload = new byte[buffer.remaining()];
		buffer.get(payload);

		return new ReplicationEnvelope(version, expiresAt, payload);
	}
}
