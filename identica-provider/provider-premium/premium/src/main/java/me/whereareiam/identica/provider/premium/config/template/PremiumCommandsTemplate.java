package me.whereareiam.identica.provider.premium.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.CommandDefinition;
import me.whereareiam.identica.provider.premium.config.PremiumCommands;

import java.util.List;

@Singleton
public class PremiumCommandsTemplate implements TemplateProvider<PremiumCommands> {
	@Override
	public PremiumCommands supply(PremiumCommands commands) {
		CommandDefinition premium = CommandDefinition.builder()
				.enabled(true)
				.aliases(List.of("premium"))
				.permission("")
				.description("Premium account migration")
				.usage("{alias}")
				.cooldown(CommandDefinition.Cooldown.builder()
						.enabled(true)
						.duration(2)
						.group("global")
						.build())
				.build();

		commands.getCommands().put("premium", premium);
		return commands;
	}
}
