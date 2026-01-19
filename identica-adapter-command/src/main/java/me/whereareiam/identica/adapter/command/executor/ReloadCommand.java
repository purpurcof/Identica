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
import org.jetbrains.annotations.NotNull;

import java.util.Map;
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
		Messages messages = messagesProvider.get();
		sender.sendMessage(Serializer.serialize(sender, messages.getCommands().getReloadStart()));
		try {
			for (Reloadable reloadable : reloadablesProvider.get()) {
				reloadable.reload();
			}
			sender.sendMessage(Serializer.serialize(sender, messages.getCommands().getReloadSuccess()));
		} catch (Exception ex) {
			sender.sendMessage(Serializer.serialize(
					sender,
					messages.getCommands().getReloadError(),
					Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())
			));
		}
	}
}
