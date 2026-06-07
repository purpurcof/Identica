package me.whereareiam.identica.feature.verification.challenge;

import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import me.whereareiam.identica.replication.codec.SnapshotCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class VerificationStateCodec {
	public <S extends PipelineStateItem> @NotNull String encode(@NotNull S state) {
		@SuppressWarnings("unchecked")
		Class<S> type = (Class<S>) state.getClass();
		SnapshotCodec<S> codec = SnapshotCodec.json(type);
		return new String(codec.encode(state), StandardCharsets.UTF_8);
	}

	public <S extends PipelineStateItem> @Nullable S decode(@Nullable String payload, @NotNull Class<S> type) {
		if (payload == null || payload.isBlank()) return null;
		try {
			SnapshotCodec<S> codec = SnapshotCodec.json(type);
			return codec.decode(payload.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ignored) {
			return null;
		}
	}
}
