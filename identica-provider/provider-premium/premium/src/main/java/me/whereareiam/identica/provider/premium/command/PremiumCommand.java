package me.whereareiam.identica.provider.premium.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.handshake.PremiumForceOnlineInstruction;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakeAttributes;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumCommand {
	private final ConnectionCoordinator connectionCoordinator;
	private final HandshakeStore handshakeStore;
	private final Provider<Settings> settingsProvider;
	private final Provider<PremiumMessages> messagesProvider;

	@Command("premium")
	@Definition("premium")
	public void onCommand(@NotNull Actor sender) {
		if (connectionCoordinator.hasPending(sender.getUniqueId())) {
			confirmPremium(sender);
			return;
		}

		startMigration(sender);
	}

	private void confirmPremium(@NotNull Actor sender) {
		requestForceOnline(sender.getUsername());
		connectionCoordinator.clearPending(sender.getUniqueId());

		disconnectWithMessage(sender, joinMessage(messagesProvider.get().getCommands().getPremium().getConfirmed()));
	}

	private void startMigration(@NotNull Actor sender) {
		// TODO
	}

	private void requestForceOnline(String username) {
		if (username == null || username.isBlank())
			return;

		long ttlMillis = settingsProvider.get()
				.getConnection()
				.handshakeInstructionTtlMillis();
		HandshakeInstruction instruction = HandshakeInstruction.create(
				new ConnectionIdentity(username, null),
				ttlMillis
		);
		instruction.setAttribute(PremiumHandshakeAttributes.FORCE_ONLINE,
				new PremiumForceOnlineInstruction("command"));
		handshakeStore.putInstruction(instruction);
	}

	private void disconnectWithMessage(@NotNull Actor sender, @NotNull String message) {
		Component component = Serializer.serialize(sender, message);
		if (sender instanceof Identity identity)
			identity.disconnect(component);
	}

	private String joinMessage(List<String> lines) {
		return String.join("\n", lines);
	}
}
