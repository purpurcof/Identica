package me.whereareiam.identica.common.migration;

import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.migration.MigrationResult;
import me.whereareiam.identica.migration.MigrationStart;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.type.migration.MigrationResultStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMigrationServiceTest {
	@Mock
	private ProviderManager providerManager;
	@Mock
	private ProviderLinkPersistenceService providerLinkPersistenceService;
	@Mock
	private AccountPersistenceService accountPersistenceService;
	@Mock
	private PipelineStateStore pipelineStateStore;
	@Mock
	private SessionService sessionService;
	@Mock
	private IdentityService identityService;

	@Test
	void deniesMigrationWhenUsernameNotFree() {
		when(pipelineStateStore.find(any(PipelineStateReference.class))).thenReturn(Optional.empty());

		Account existing = Account.builder()
				.uniqueId(UUID.randomUUID())
				.username("Player")
				.source(UsernameSource.SYSTEM)
				.build();
		when(accountPersistenceService.findByUsername("Player")).thenReturn(List.of(existing));

		Messages messages = new Messages();
		Messages.Commands commandsMessages = new Messages.Commands();
		Messages.Commands.Migration migrationMessages = new Messages.Commands.Migration();
		migrationMessages.setLocked("locked");
		commandsMessages.setMigration(migrationMessages);
		messages.setCommands(commandsMessages);

		Commands commands = new Commands();
		Commands.Behavior behavior = new Commands.Behavior();
		Commands.Behavior.Migration migrationBehavior = new Commands.Behavior.Migration();
		migrationBehavior.setConfirmTtl(Duration.ofSeconds(60));
		behavior.setMigration(migrationBehavior);
		behavior.setSuggestions(new Commands.Behavior.Suggestions());
		behavior.setClear(new Commands.Behavior.Clear());
		behavior.setSessions(new Commands.Behavior.Sessions());
		commands.setBehavior(behavior);

		DefaultMigrationService service = new DefaultMigrationService(
				providerManager,
				providerLinkPersistenceService,
				accountPersistenceService,
				pipelineStateStore,
				sessionService,
				identityService,
				Settings::new,
				() -> commands,
				() -> messages
		);

		UUID uniqueId = UUID.randomUUID();
		MigrationResult result = service.start(MigrationStart.builder()
				.uniqueId(uniqueId)
				.connectionUniqueId(uniqueId)
				.username("Player")
				.targetProviderId("provider")
				.build());

		assertEquals(MigrationResultStatus.PRECHECK_DENIED, result.getStatus());
		assertEquals("locked", result.getMessage());
	}
}
