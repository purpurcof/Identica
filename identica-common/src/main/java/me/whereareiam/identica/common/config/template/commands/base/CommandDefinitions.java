package me.whereareiam.identica.common.config.template.commands.base;

import me.whereareiam.identica.model.CommandDefinition;
import org.jetbrains.annotations.NotNull;

public interface CommandDefinitions {
	void register(@NotNull Registry registry);

	default CommandDefinition.Cooldown globalCooldown() {
		return CommandDefinition.Cooldown.builder()
				.enabled(true)
				.duration(2)
				.group("global")
				.build();
	}

	interface Registry {
		void register(@NotNull String key, @NotNull CommandDefinition definition);
	}
}
