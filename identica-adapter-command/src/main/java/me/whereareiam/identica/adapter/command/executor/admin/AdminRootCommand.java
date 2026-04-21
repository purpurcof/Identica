package me.whereareiam.identica.adapter.command.executor.admin;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.adapter.command.executor.HelpCommand;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.keystone.Actor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminRootCommand {
	private final HelpCommand helpCommand;

	@Definition("admin")
	@Command("identica admin")
	public void command(@NotNull Actor sender) {
		helpCommand.command(sender, 1);
	}
}
