package me.whereareiam.identica.adapter.command.executor;

import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.migration.operation.MigrationResult;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.service.MigrationService;
import me.whereareiam.identica.type.migration.MigrationResultStatus;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import me.whereareiam.keystone.serializer.SerializerEngine;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Migration Command")
class MigrationCommandTest {
	@BeforeAll
	static void initializeSerializer() {
		Serializer.initialize(() -> TEST_SERIALIZER);
	}

	private static final SerializerEngine TEST_SERIALIZER = new SerializerEngine() {
		@Override
		public @NotNull String serialize(@NotNull Component component) {
			return component.toString();
		}

		@Override
		public @NotNull Component serialize(@NotNull SerializerContent content) {
			return Component.text(content.getMessage());
		}

		public @NotNull String renderTemplate(@NotNull SerializerContent content) {
			return content.getMessage();
		}

		@Override
		public @NotNull SerializerOptions.PlaceholderFormat getPlaceholderFormat() {
			return SerializerOptions.PlaceholderFormat.CURLY_BRACES;
		}
	};

	@Mock
	private AccountPersistenceService accountPersistenceService;
	@Mock
	private ProviderLinkPersistenceService providerLinkPersistenceService;
	@Mock
	private ProviderProfilePersistenceService providerProfilePersistenceService;
	@Mock
	private ProviderManager providerManager;
	@Mock
	private MigrationService migrationService;
	@Mock
	private IdentityService identityService;

	@Test
	@DisplayName("Shows provider unsupported when the service rejects the target")
	void startShowsProviderUnsupported() {
		TestActor sender = new TestActor();
		Account target = account();
		when(accountPersistenceService.findByUniqueId(target.getUniqueId())).thenReturn(Optional.of(target));
		when(migrationService.start(any())).thenReturn(MigrationResult.builder()
				.status(MigrationResultStatus.TARGET_UNSUPPORTED)
				.build());

		MigrationCommand command = new MigrationCommand(
				this::messages,
				accountPersistenceService,
				providerLinkPersistenceService,
				providerProfilePersistenceService,
				providerManager,
				migrationService,
				identityService
		);

		command.start(sender, target.getUniqueId().toString(), "premium");

		assertEquals("unsupported", PlainTextComponentSerializer.plainText().serialize(sender.lastMessage()));
	}

	@Test
	@DisplayName("Shows provider unavailable when the service rejects an inactive target")
	void startShowsProviderUnavailable() {
		TestActor sender = new TestActor();
		Account target = account();
		when(accountPersistenceService.findByUniqueId(target.getUniqueId())).thenReturn(Optional.of(target));
		when(migrationService.start(any())).thenReturn(MigrationResult.builder()
				.status(MigrationResultStatus.PROVIDER_UNAVAILABLE)
				.build());

		MigrationCommand command = new MigrationCommand(
				this::messages,
				accountPersistenceService,
				providerLinkPersistenceService,
				providerProfilePersistenceService,
				providerManager,
				migrationService,
				identityService
		);

		command.start(sender, target.getUniqueId().toString(), "premium");

		assertEquals("unavailable", PlainTextComponentSerializer.plainText().serialize(sender.lastMessage()));
	}

	private Account account() {
		return Account.builder()
				.uniqueId(UUID.randomUUID())
				.username("PlayerOne")
				.build();
	}

	private Messages messages() {
		Messages messages = new Messages();
		Messages.Commands commands = new Messages.Commands();
		Messages.Commands.Migration migration = new Messages.Commands.Migration();
		Messages.Commands.Migration.Start start = new Messages.Commands.Migration.Start();
		start.setProviderUnsupported("unsupported");
		start.setProviderUnavailable("unavailable");
		migration.setStart(start);
		commands.setMigration(migration);
		messages.setCommands(commands);
		return messages;
	}

	private static final class TestActor implements Actor {
		private final AtomicReference<Component> lastMessage = new AtomicReference<>();

		@Override
		public @NotNull UUID getUniqueId() {
			return UUID.randomUUID();
		}

		@Override
		public @NotNull String getUsername() {
			return "Admin";
		}

		@Override
		public void sendMessage(@NotNull Component message) {
			lastMessage.set(message);
		}

		@Override
		public boolean hasPermission(@NotNull String permission) {
			return true;
		}

		@Override
		public @NotNull Locale getLocale() {
			return Locale.ENGLISH;
		}

		@Override
		public @NotNull Audience getAudience() {
			return Audience.empty();
		}

		private Component lastMessage() {
			return lastMessage.get();
		}
	}
}
