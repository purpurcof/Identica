package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.keystone.Actor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MainCommand {
	private final HelpCommand helpCommand;

	@Definition("main")
	@Command("identica")
	public void command(@NotNull Actor sender) {
		helpCommand.command(sender, 0);
	}
}
