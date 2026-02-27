package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AvailabilityCommand {
	private final AccountPersistenceService accountPersistenceService;
	private final Provider<Messages> messagesProvider;

	@Definition("availability-username")
	@Command("identica availability username <username>")
	public void availability(
			@NotNull Actor sender,
			@Argument("username") String username
	) {
		boolean taken = isTaken(username);
		Messages.Commands.Availability.Username messages = messagesProvider.get()
				.getCommands()
				.getAvailability()
				.getUsername();
		String message = taken
				? messages.getTaken()
				: messages.getFree();

		sendMessage(sender, message, Map.of("username", username));
	}

	private boolean isTaken(@NotNull String username) {
		List<Account> accounts = accountPersistenceService.findByUsername(username);
		if (accounts.isEmpty()) return false;

		for (Account account : accounts) {
			if (account == null) continue;
			if (username.equalsIgnoreCase(account.getUsername()))
				return true;
		}

		return false;
	}

	private void sendMessage(@NotNull Actor sender, @NotNull String message, @NotNull Map<String, String> placeholders) {
		if (message.isBlank()) return;
		Component component = Serializer.serialize(sender, message, placeholders);
		sender.sendMessage(component);
	}
}
