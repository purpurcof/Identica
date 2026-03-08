package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.pipeline.journey.registry.AuthenticationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.MigrationJourneyRegistry;
import me.whereareiam.identica.pipeline.journey.registry.RegistrationJourneyRegistry;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.model.provider.ResolvedEntrypoint;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.provider.ProviderState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

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

	@Test
	void resolvesConfiguredProviderDisplayName() {
		Providers providers = new Providers();
		Providers.ProviderEntry entry = entry("alpha", 10, List.of("play.example.com"));
		entry.setDisplayName("Alpha Network");
		providers.setProviders(List.of(entry));

		ProviderOperations operations = operations(providers);
		assertEquals("Alpha Network", operations.displayProviderName("alpha"));
	}

	@Test
	void fallsBackToDescriptorNameWhenConfiguredDisplayNameMissing() {
		Providers providers = new Providers();
		providers.setProviders(List.of(entry("alpha", 10, List.of("play.example.com"))));

		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId("alpha");
		descriptor.setName("Alpha Provider");

		InternalProvider provider = InternalProvider.builder()
				.descriptor(descriptor)
				.state(ProviderState.ENABLED)
				.build();
		when(providerManager.getProviders()).thenReturn(List.of(provider));

		ProviderOperations operations = operations(providers);
		assertEquals("Alpha Provider", operations.displayProviderName("alpha"));
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
