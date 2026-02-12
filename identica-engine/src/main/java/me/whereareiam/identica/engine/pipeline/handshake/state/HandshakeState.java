package me.whereareiam.identica.engine.pipeline.handshake.state;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public final class HandshakeState {
	private final @Nullable HandshakeRequest request;
	private final @Nullable String username;
	private @Nullable HandshakeDecision decision;

	public HandshakeState(@Nullable HandshakeRequest request) {
		this.request = request;
		this.username = request != null
				? request.getIdentity().getUsername()
				: null;

		this.decision = HandshakeDecision.allow();
	}

	public @NotNull HandshakeDecision resolvedDecision() {
		return decision != null ? decision : HandshakeDecision.allow();
	}
}
