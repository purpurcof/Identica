package me.whereareiam.identica.engine.pipeline.handshake.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class HandshakeRequestState implements PipelineStateItem {
	private @Nullable ConnectionIdentity identity;

	public static @Nullable HandshakeRequestState from(@Nullable HandshakeRequest request) {
		if (request == null) return null;
		return new HandshakeRequestState(request.getIdentity());
	}

	public @Nullable HandshakeRequest toRequest() {
		if (identity == null) {
			return null;
		}
		return new HandshakeRequest(identity);
	}
}
