package me.whereareiam.identica.provider.premium.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.actor.Identity;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.type.HandshakeMode;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumCommand {
	private final AuthCoordinator authCoordinator;
	private final Provider<PremiumMessages> messagesProvider;

	@Command("premium")
	@Definition("premium")
	public void onCommand(@NotNull Actor sender) {
		if (authCoordinator.hasPending(sender.getUniqueId())) {
			confirmPremium(sender);
			return;
		}

		startMigration(sender);
	}

	private void confirmPremium(@NotNull Actor sender) {
		authCoordinator.requestHandshakeDirective(sender.getUsername(), HandshakeMode.ONLINE);
		authCoordinator.clearPending(sender.getUniqueId());

		disconnectWithMessage(sender, joinMessage(messagesProvider.get().getCommands().getPremium().getConfirmed()));
	}

	private void startMigration(@NotNull Actor sender) {
	}

	private void disconnectWithMessage(@NotNull Actor sender, @NotNull String message) {
		Component component = Serializer.serialize(sender, message);
		if (sender instanceof Identity identity)
			identity.disconnect(component);
	}

	private String joinMessage(List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
