package me.whereareiam.identica.platform.velocity.mapper;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandConsole;
import me.whereareiam.identica.platform.velocity.actor.VelocityCommandPlayer;
import me.whereareiam.keystone.Actor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;

public class CommandSourceMapper implements SenderMapper<CommandSource, Actor> {
	@Override
	public @NonNull Actor map(@NonNull CommandSource source) {
		if (source instanceof ConsoleCommandSource console)
			return new VelocityCommandConsole(console);

		if (source instanceof Player player)
			return new VelocityCommandPlayer(player);

		throw new UnsupportedOperationException("Unsupported command source type: " + source.getClass().getName());
	}

	@Override
	public @NonNull CommandSource reverse(@NonNull Actor actor) {
		if (actor instanceof VelocityCommandPlayer player)
			return player.getSource();

		if (actor instanceof VelocityCommandConsole console)
			return console.getSource();

		throw new UnsupportedOperationException("Cannot reverse map Actor to CommandSource");
	}
}
