package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.Serializer;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ReloadCommand {
	private final Provider<Set<Reloadable>> reloadablesProvider;
	private final Provider<Messages> messagesProvider;

	@Inject
	public ReloadCommand(
			@Named("reloadables") Provider<Set<Reloadable>> reloadablesProvider,
			Provider<Messages> messagesProvider
	) {
		this.reloadablesProvider = reloadablesProvider;
		this.messagesProvider = messagesProvider;
	}

	@Definition("reload")
	@Command("identica reload")
	public void command(@NotNull Actor sender) {
		Messages.Commands.Reload reload = messagesProvider.get().getCommands().getReload();

		try {
			// Reload all registered reloadable components
			Set<Reloadable> reloadables = reloadablesProvider.get();
			for (Reloadable reloadable : reloadables)
				reloadable.reload();

			reload = messagesProvider.get().getCommands().getReload();

			Component component = Serializer.serialize(sender, reload.getSuccess());
			sender.sendMessage(component);
		} catch (Exception e) {
			Component component = Serializer.serialize(SerializerContent.builder()
					.receiver(sender)
					.message(reload.getError())
					.placeholder("error", e.getMessage())
					.build());
			sender.sendMessage(component);
		}
	}
}
