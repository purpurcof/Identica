package me.whereareiam.identica.actor;

import me.whereareiam.keystone.Actor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface Identity extends Actor {
	void disconnect(@NotNull Component reason);
}
