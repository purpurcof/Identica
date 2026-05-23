package me.whereareiam.identica.command;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

@SuppressWarnings("unused")
public interface CommandDefinitionCollector {
	@NotNull Set<String> getAllowedDuringAuthAliases();
}
