package me.whereareiam.identica.platform.bungeecord.listener.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.command.CommandFilterService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.keystone.model.SerializerContent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CommandBlockerListener implements DynamicListener<ChatEvent> {
	private final CommandFilterService commandFilterService;
	private final IdentityService identityService;
	private final Provider<Messages> messagesProvider;

	@Inject
	public CommandBlockerListener(
			CommandFilterService commandFilterService,
			IdentityService identityService,
			Provider<Messages> messagesProvider
	) {
		this.commandFilterService = commandFilterService;
		this.identityService = identityService;
		this.messagesProvider = messagesProvider;
	}

	@Override
	public void onEvent(@NotNull ChatEvent event) {
		if (!event.isCommand())
			return;

		if (!(event.getSender() instanceof ProxiedPlayer player))
			return;

		String message = event.getMessage();
		if (message == null || message.isBlank())
			return;

		Identity identity = identityService.findByConnectionUniqueId(player.getUniqueId()).orElse(null);
		if (identity == null)
			return;

		if (commandFilterService.isAllowed(identity, message))
			return;

		event.setCancelled(true);

		String blockedMessage = messagesProvider.get().getCommands().getCommandBlockedDuringAuth();
		if (!blockedMessage.isBlank()) {
			SerializerContent content = SerializerContent.builder()
					.receiver(identity)
					.message(blockedMessage)
					.build();
			identity.sendMessage(Serializer.serialize(content));
		}
	}
}
