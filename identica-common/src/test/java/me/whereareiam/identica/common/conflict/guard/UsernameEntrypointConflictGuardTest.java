package me.whereareiam.identica.common.conflict.guard;

import me.whereareiam.identica.common.config.template.messages.MessagesTemplate;
import me.whereareiam.identica.common.provider.DefaultProviderOperations;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.pipeline.journey.registry.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.MigrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.RegistrationJourneyRegistry;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class UsernameEntrypointConflictGuardTest {
	@Mock
	private ProviderManager providerManager;
	@Mock
	private AuthenticationJourneyRegistry authenticationJourneyRegistry;
	@Mock
	private RegistrationJourneyRegistry registrationJourneyRegistry;
	@Mock
	private MigrationJourneyRegistry migrationJourneyRegistry;
	@Mock
	private EventManager eventManager;

	@Test
	void deniesWhenEntrypointAmbiguous() {
		Providers providers = new Providers();
		providers.setProviders(List.of(
				entry("premium", "Premium Network", 100, List.of("premium.example.com")),
				entry("cracked", "Offline Network", 50, List.of("cracked.example.com"))
		));
		Messages messages = new MessagesTemplate().supply(new Messages());

		ProviderOperations providerOperations = providerOperations(providers);
		UsernameEntrypointConflictGuard guard = new UsernameEntrypointConflictGuard(
				() -> messages,
				providerOperations
		);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("Player")
				.incomingLink(link("premium"))
				.existingLink(link("cracked"))
				.build();
		context.putExtra("entrypointSource", ProviderOrigin.AUTO);

		ConflictResolution resolution = guard.guard(context);
		assertNotNull(resolution);
		assertEquals(ConflictResolution.Action.DENY, resolution.getAction());
		assertNotNull(resolution.getMessage());
		assertTrue(resolution.getMessage().contains("Premium Network"));
		assertTrue(resolution.getMessage().contains("Offline Network"));
		assertTrue(resolution.getMessage().contains("premium.example.com"));
		assertTrue(resolution.getMessage().contains("cracked.example.com"));
	}

	@Test
	void allowsWhenEntrypointSelected() {
		Providers providers = new Providers();
		providers.setProviders(List.of(
				entry("premium", "Premium Network", 100, List.of("premium.example.com")),
				entry("cracked", "Offline Network", 50, List.of("cracked.example.com"))
		));

		ProviderOperations providerOperations = providerOperations(providers);
		UsernameEntrypointConflictGuard guard = new UsernameEntrypointConflictGuard(
				Messages::new,
				providerOperations
		);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("Player")
				.incomingLink(link("premium"))
				.existingLink(link("cracked"))
				.build();
		context.putExtra("entrypointSource", ProviderOrigin.ENTRYPOINT);

		assertNull(guard.guard(context));
	}

	private Providers.ProviderEntry entry(String id, String displayName, int priority, List<String> entrypoints) {
		Providers.ProviderEntry entry = new Providers.ProviderEntry();
		entry.setId(id);
		entry.setDisplayName(displayName);
		entry.setPriority(priority);
		entry.setEntrypoints(entrypoints);
		return entry;
	}

	private AccountProviderLink link(String providerId) {
		return AccountProviderLink.builder()
				.uniqueId(UUID.randomUUID())
				.providerId(providerId)
				.providerSubject("subject-" + providerId)
				.primaryLink(true)
				.build();
	}

	private ProviderOperations providerOperations(Providers providers) {
		return new DefaultProviderOperations(
				providerManager,
				authenticationJourneyRegistry,
				registrationJourneyRegistry,
				migrationJourneyRegistry,
				() -> providers,
				eventManager
		);
	}
}
