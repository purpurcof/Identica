package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EnrollCommand {
	private final AuthenticationCoordinator authenticationCoordinator;

	@Definition("enroll")
	@Command("identica enroll <eligibility>")
	public void enroll(@NotNull Actor sender, @Argument("eligibility") String providerId) {
		if (providerId == null || providerId.isBlank())
			return;

		AuthDecision decision = authenticationCoordinator.resume(sender.getUniqueId(), context -> {
			if (context == null) return;

			AuthContext.Provider provider = context.getProvider();
			if (provider == null) {
				String username = context.getUsername() != null ? context.getUsername() : "";
				context.setProvider(AuthContext.Provider.builder()
						.providerId(providerId)
						.providerUsername(username)
						.build());
				return;
			}

			provider.setProviderId(providerId);
		}).toCompletableFuture().join();

		if (decision == null || decision.getStatus() == null)
			return;

		switch (decision.getStatus()) {
			case WAIT -> sendMessage(sender, decision.getMessage());
			case DENY, REQUIRE_RECONNECT -> disconnect(sender, decision.getMessage());
			default -> {
			}
		}
	}

	private void sendMessage(@NotNull Actor sender, String message) {
		if (message == null || message.isBlank()) return;
		sender.sendMessage(Serializer.serialize(sender, message));
	}

	private void disconnect(@NotNull Actor sender, String message) {
		if (message == null || message.isBlank()) return;
		Component component = Serializer.serialize(sender, message);
		if (sender instanceof Identity identity)
			identity.disconnect(component);
	}
}
