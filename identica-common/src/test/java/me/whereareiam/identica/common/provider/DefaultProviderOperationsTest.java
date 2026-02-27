package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.pipeline.journey.registry.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.MigrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.RegistrationJourneyRegistry;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.model.provider.ResolvedEntrypoint;
import me.whereareiam.identica.provider.ProviderOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class DefaultProviderOperationsTest {
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
	void resolvesExactHostMatch() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("play.example.com"))));

		ProviderOperations operations = operations(providers);
		ResolvedEntrypoint resolved =
				operations.resolveEntrypoint("Play.Example.Com", 25565);

		assertNotNull(resolved);
		assertEquals("alpha", resolved.getProviderId());
	}

	@Test
	void resolvesHostAndPortMatch() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("game.example.com:25570"))));

		ProviderOperations operations = operations(providers);
		assertNull(operations.resolveEntrypoint("game.example.com", 25565));

		ResolvedEntrypoint resolved =
				operations.resolveEntrypoint("game.example.com", 25570);

		assertNotNull(resolved);
		assertEquals("alpha", resolved.getProviderId());
	}

	@Test
	void resolvesHighestPriorityThenProviderId() {
		Providers providers = new Providers();
		Providers.ProviderEntry low = entry("beta", 50, List.of("shared.example.com"));
		Providers.ProviderEntry high = entry("alpha", 100, List.of("shared.example.com"));
		providers.setProviders(List.of(low, high));

		ProviderOperations operations = operations(providers);
		ResolvedEntrypoint resolved =
				operations.resolveEntrypoint("shared.example.com", 25565);

		assertNotNull(resolved);
		assertEquals("alpha", resolved.getProviderId());
	}

	private ProviderOperations operations(Providers providers) {
		return new DefaultProviderOperations(
				providerManager,
				authenticationJourneyRegistry,
				registrationJourneyRegistry,
				migrationJourneyRegistry,
				() -> providers,
				eventManager
		);
	}

	private Providers.ProviderEntry entry(String id, int priority, List<String> entrypoints) {
		Providers.ProviderEntry entry = new Providers.ProviderEntry();
		entry.setId(id);
		entry.setPriority(priority);
		entry.setEntrypoints(entrypoints);
		return entry;
	}
}
