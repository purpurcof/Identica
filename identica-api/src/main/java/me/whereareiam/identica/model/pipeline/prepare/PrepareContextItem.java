package me.whereareiam.identica.model.pipeline.prepare;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepareContextItem implements PipelineStateItem {
	private @Nullable ProviderContext provider;
	private @Nullable HandshakeDecision.Status handshakeStatus;
	private @Nullable String handshakeMessage;

	public void applyHandshake(@Nullable HandshakeDecision decision) {
		if (decision == null) {
			handshakeStatus = null;
			handshakeMessage = null;
			return;
		}

		handshakeStatus = decision.getStatus();
		handshakeMessage = decision.getMessage();
	}

	public @Nullable HandshakeDecision resolveHandshake() {
		if (handshakeStatus == null) return null;
		return new HandshakeDecision(handshakeStatus, handshakeMessage, null);
	}
}
