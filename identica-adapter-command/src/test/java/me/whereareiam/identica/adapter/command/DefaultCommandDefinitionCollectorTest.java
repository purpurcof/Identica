package me.whereareiam.identica.adapter.command;

import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.model.CommandDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Command Definition Collector")
class DefaultCommandDefinitionCollectorTest {
	private final CommandService commandService = mock(CommandService.class);
	private final DefaultCommandDefinitionCollector collector = new DefaultCommandDefinitionCollector(commandService);

	@DisplayName("Returns empty set when no definitions are allowed during auth")
	@Test
	void returnsEmptySetWhenNoneAllowed() {
		CommandDefinition def = CommandDefinition.builder()
				.aliases(List.of("tp", "teleport"))
				.allowedDuringAuth(false)
				.build();
		when(commandService.getRegisteredDefinitions()).thenReturn(Map.of("teleport", def));

		assertTrue(collector.getAllowedDuringAuthAliases().isEmpty());
	}

	@DisplayName("Returns aliases from definitions marked as allowedDuringAuth")
	@Test
	void returnsAllowedAliases() {
		CommandDefinition login = CommandDefinition.builder()
				.aliases(List.of("login", "l"))
				.allowedDuringAuth(true)
				.build();
		CommandDefinition tp = CommandDefinition.builder()
				.aliases(List.of("tp"))
				.allowedDuringAuth(false)
				.build();
		when(commandService.getRegisteredDefinitions()).thenReturn(Map.of("login", login, "tp", tp));

		Set<String> aliases = collector.getAllowedDuringAuthAliases();

		assertEquals(Set.of("login", "l"), aliases);
	}

	@DisplayName("Caches result on subsequent calls")
	@Test
	void cachesResult() {
		CommandDefinition def = CommandDefinition.builder()
				.aliases(List.of("login"))
				.allowedDuringAuth(true)
				.build();
		when(commandService.getRegisteredDefinitions()).thenReturn(Map.of("login", def));

		Set<String> first = collector.getAllowedDuringAuthAliases();
		Set<String> second = collector.getAllowedDuringAuthAliases();

		assertSame(first, second);
	}

	@DisplayName("Invalidates cache when invalidateCache is called")
	@Test
	void invalidatesCache() {
		CommandDefinition def = CommandDefinition.builder()
				.aliases(List.of("login"))
				.allowedDuringAuth(true)
				.build();
		when(commandService.getRegisteredDefinitions()).thenReturn(Map.of("login", def));

		collector.getAllowedDuringAuthAliases();
		collector.invalidateCache();

		when(commandService.getRegisteredDefinitions()).thenReturn(Map.of(
				"login", def,
				"register", CommandDefinition.builder()
						.aliases(List.of("register"))
						.allowedDuringAuth(true)
						.build()
		));

		assertEquals(Set.of("login", "register"), collector.getAllowedDuringAuthAliases());
	}

	@DisplayName("Returns unmodifiable set")
	@Test
	void returnsUnmodifiableSet() {
		CommandDefinition def = CommandDefinition.builder()
				.aliases(List.of("login"))
				.allowedDuringAuth(true)
				.build();
		when(commandService.getRegisteredDefinitions()).thenReturn(Map.of("login", def));

		Set<String> aliases = collector.getAllowedDuringAuthAliases();

		assertThrows(UnsupportedOperationException.class, () -> aliases.add("newalias"));
	}

	@DisplayName("Ignores definitions with null aliases")
	@Test
	void ignoresNullAliases() {
		CommandDefinition def = CommandDefinition.builder()
				.allowedDuringAuth(true)
				.build();
		when(commandService.getRegisteredDefinitions()).thenReturn(Map.of("test", def));

		assertTrue(collector.getAllowedDuringAuthAliases().isEmpty());
	}
}
