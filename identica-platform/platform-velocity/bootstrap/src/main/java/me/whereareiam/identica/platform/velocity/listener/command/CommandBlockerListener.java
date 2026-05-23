package me.whereareiam.identica.platform.velocity.listener.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.command.CommandFilterService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CommandBlockerListener implements DynamicListener<CommandExecuteEvent> {
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
	public void onEvent(@NotNull CommandExecuteEvent event) {
		if (!(event.getCommandSource() instanceof Player player))
			return;

		String commandLine = event.getCommand();
		if (commandLine == null || commandLine.isBlank())
			return;

		Identity identity = identityService.findByConnectionUniqueId(player.getUniqueId()).orElse(null);
		if (identity == null)
			return;

		if (commandFilterService.isAllowed(identity, commandLine))
			return;

		event.setResult(CommandExecuteEvent.CommandResult.denied());

		String message = messagesProvider.get().getCommands().getCommandBlockedDuringAuth();
		if (!message.isBlank()) {
			SerializerContent content = SerializerContent.builder()
					.receiver(identity)
					.message(message)
					.build();
			identity.sendMessage(Serializer.serialize(content));
		}
	}
}
