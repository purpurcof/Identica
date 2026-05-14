package me.whereareiam.identica.platform.bungeecord.adapter.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.adapter.HandshakeDecisionProcessor;
import me.whereareiam.identica.handshake.HandshakeApplierRegistry;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.platform.bungeecord.api.handshake.BungeeCordHandshakeContext;
import me.whereareiam.identica.platform.bungeecord.util.BaseComponentMapper;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BungeeCordHandshakeDecisionAdapter {
	private final @NotNull HandshakeDecisionProcessor processor;
	private final @NotNull HandshakeApplierRegistry<BungeeCordHandshakeContext> applierRegistry;

	public @NotNull CompletionStage<Void> process(@NotNull PreLoginEvent event) {
		Request request = request(event);
		if (request == null) return CompletableFuture.completedFuture(null);

		return processor.process(toProcessorRequest(request), message -> {
			event.setCancelled(true);
			event.setReason(TextComponent.fromArray(BaseComponentMapper.map(message)));
		}).toCompletableFuture();
	}

	private @Nullable Request request(@NotNull PreLoginEvent event) {
		String username = event.getConnection().getName();
		String resolvedIp = resolveIp(event);
		if (username == null || username.isBlank() || resolvedIp == null) {
			Logger.debug("Bungee handshake missing connection data username=%s ip=%s, skipping handshake processing", username, resolvedIp);
			return null;
		}

		ConnectionIdentity identity = new ConnectionIdentity(username, resolvedIp);
		applyOrigin(identity, event);
		BungeeCordHandshakeContext context = new BungeeCordHandshakeContext(event.getConnection());
		return new Request(
				identity,
				instruction -> applierRegistry.applyAll(context, instruction)
		);
	}

	private @NotNull HandshakeDecisionProcessor.Request toProcessorRequest(@NotNull Request request) {
		return new HandshakeDecisionProcessor.Request(request.getIdentity(), request.getInstructionTarget());
	}

	private void applyOrigin(@NotNull ConnectionIdentity identity, @NotNull PreLoginEvent event) {
		if (event.getConnection().getVirtualHost() == null) return;
		identity.setOrigin(new ConnectionIdentity.Origin(
				event.getConnection().getVirtualHost().getHostString(),
				event.getConnection().getVirtualHost().getPort()
		));
	}

	private @Nullable String resolveIp(@NotNull PreLoginEvent event) {
		SocketAddress address = event.getConnection().getSocketAddress();
		if (!(address instanceof InetSocketAddress inetSocketAddress))
			return null;
		if (inetSocketAddress.getAddress() != null)
			return inetSocketAddress.getAddress().getHostAddress();

		return inetSocketAddress.getHostString();
	}

	@Getter
	@RequiredArgsConstructor
	private static final class Request {
		private final @NotNull ConnectionIdentity identity;
		private final @NotNull HandshakeDecisionProcessor.InstructionTarget instructionTarget;
	}
}
