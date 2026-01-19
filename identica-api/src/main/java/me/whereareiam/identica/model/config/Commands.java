package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.CommandDefinition;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class Commands {
	private Map<String, CommandDefinition> commands = new HashMap<>();
}
