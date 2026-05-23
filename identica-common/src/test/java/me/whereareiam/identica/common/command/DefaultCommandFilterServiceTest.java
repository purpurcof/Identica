package me.whereareiam.identica.common.command;

import me.whereareiam.identica.command.CommandDefinitionCollector;
import me.whereareiam.identica.command.CommandService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.keystone.Actor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Command Filter Service")
class DefaultCommandFilterServiceTest {
	private final SessionService sessionService = mock(SessionService.class);
	private final CommandDefinitionCollector definitionCollector = mock(CommandDefinitionCollector.class);
	private final CommandService commandService = mock(CommandService.class);
	private final DefaultCommandFilterService service = new DefaultCommandFilterService(sessionService, definitionCollector, commandService);

	@DisplayName("Allows non-Identity actors (console)")
	@Test
	void allowsNonIdentityActors() {
		Actor console = mock(Actor.class);

		assertTrue(service.isAllowed(console, "/somecommand"));
	}

	@DisplayName("Allows identity with active session")
	@Test
	void allowsIdentityWithActiveSession() {
		UUID accountId = UUID.randomUUID();
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(accountId);
		when(sessionService.findByUniqueId(accountId)).thenReturn(CompletableFuture.completedFuture(Optional.of(mock(Session.class))));

		assertTrue(service.isAllowed(identity, "/somecommand"));
	}

	@DisplayName("Allows identity with null accountUniqueId when command is whitelisted")
	@Test
	void allowsIdentityWithNullAccountAndWhitelistedCommand() {
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(null);
		when(definitionCollector.getAllowedDuringAuthAliases()).thenReturn(Set.of("login"));

		assertTrue(service.isAllowed(identity, "/login"));
	}

	@DisplayName("Denies identity with null accountUniqueId when command is not whitelisted")
	@Test
	void deniesIdentityWithNullAccountAndNonWhitelistedCommand() {
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(null);
		when(definitionCollector.getAllowedDuringAuthAliases()).thenReturn(Set.of("login"));

		assertFalse(service.isAllowed(identity, "/tp"));
	}

	@DisplayName("Denies identity without session when command is not whitelisted")
	@Test
	void deniesIdentityWithoutSessionAndNonWhitelistedCommand() {
		UUID accountId = UUID.randomUUID();
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(accountId);
		when(sessionService.findByUniqueId(accountId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(definitionCollector.getAllowedDuringAuthAliases()).thenReturn(Set.of("login"));

		assertFalse(service.isAllowed(identity, "/tp"));
	}

	@DisplayName("Handles command with subcommands by checking only the first word")
	@Test
	void handlesSubcommandByCheckingFirstWord() {
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(null);
		when(definitionCollector.getAllowedDuringAuthAliases()).thenReturn(Set.of("2fa"));
		when(commandService.getRegisteredDefinitions()).thenReturn(new HashMap<>());

		assertTrue(service.isAllowed(identity, "/2fa enroll totp"));
		assertTrue(service.isAllowed(identity, "/2fa status"));
		assertFalse(service.isAllowed(identity, "/other subcommand"));
	}

	@DisplayName("Handles command without leading slash")
	@Test
	void handlesCommandWithoutSlash() {
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(null);
		when(definitionCollector.getAllowedDuringAuthAliases()).thenReturn(Set.of("login"));
		when(commandService.getRegisteredDefinitions()).thenReturn(new HashMap<>());

		assertTrue(service.isAllowed(identity, "login password123"));
	}

	@DisplayName("Returns false for blank command line")
	@Test
	void deniesBlankCommandLine() {
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(null);

		assertFalse(service.isAllowed(identity, ""));
		assertFalse(service.isAllowed(identity, " "));
	}

	@DisplayName("Returns false for null command line")
	@Test
	@SuppressWarnings("DataFlowIssue")
	void deniesNullCommandLine() {
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(null);

		assertFalse(service.isAllowed(identity, null));
	}

	@DisplayName("Checks session only when accountUniqueId is not null")
	@Test
	void doesNotCheckSessionWhenAccountUniqueIdIsNull() {
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(null);
		when(definitionCollector.getAllowedDuringAuthAliases()).thenReturn(Set.of("login"));

		assertTrue(service.isAllowed(identity, "/login"));
	}

	@DisplayName("Denies when session lookup fails and command is not whitelisted")
	@Test
	void deniesWhenSessionMissingAndCommandNotWhitelisted() {
		UUID accountId = UUID.randomUUID();
		Identity identity = mock(Identity.class);
		when(identity.getAccountUniqueId()).thenReturn(accountId);
		when(sessionService.findByUniqueId(accountId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
		when(definitionCollector.getAllowedDuringAuthAliases()).thenReturn(Set.of());

		assertFalse(service.isAllowed(identity, "/anycommand"));
	}
}
