package me.whereareiam.identica.feature.verification.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.identica.model.CommandDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Verification feature command definition document.
 */
@Getter
@Setter
@ToString
public class VerificationCommands extends ConfigDocument {
	private @NotNull Map<String, CommandDefinition> commands = new HashMap<>();
}
