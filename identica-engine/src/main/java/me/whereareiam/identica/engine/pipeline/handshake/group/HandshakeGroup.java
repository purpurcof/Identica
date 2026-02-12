package me.whereareiam.identica.engine.pipeline.handshake.group;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.handshake.state.HandshakeRequestState;
import me.whereareiam.identica.engine.pipeline.handshake.state.HandshakeState;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.group.GroupOutcome;
import me.whereareiam.identica.pipeline.group.PipelineGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class HandshakeGroup implements PipelineGroup<HandshakeState> {
	@Override
	public @NotNull String id() {
		return "handshake";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull Class<HandshakeState> stateType() {
		return HandshakeState.class;
	}

	@Override
	public @NotNull HandshakeState initializeState(
			@NotNull PipelineState pipelineState,
			@Nullable PipelineResult currentResult
	) {
		HandshakeRequest request = pipelineState.item(HandshakeRequestState.class)
				.map(HandshakeRequestState::toRequest)
				.orElse(null);
		return new HandshakeState(request);
	}

	@Override
	public @NotNull GroupOutcome complete(
			@NotNull PipelineState pipelineState,
			@NotNull HandshakeState state
	) {
		return GroupOutcome.none();
	}
}
