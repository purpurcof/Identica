package me.whereareiam.identica.command;

import me.whereareiam.keystone.Actor;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface CommandFilterService {
	boolean isAllowed(@NotNull Actor sender, @NotNull String commandLine);
}
