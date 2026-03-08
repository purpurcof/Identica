package me.whereareiam.identica.common.conflict.resolver.username;

import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.provider.ProviderOperations;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormatUsernameConflictResolverTest {
	@Test
	void formatsUsernameWhenPatternProvided() {
		ProviderOperations providerOperations = mock(ProviderOperations.class);
		FormatUsernameConflictResolver resolver = new FormatUsernameConflictResolver(providerOperations);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("Player")
				.incomingLink(link("premium"))
				.existingLink(link("cracked"))
				.build();

		FormatUsernameConflictResolver.Config config = new FormatUsernameConflictResolver.Config();
		config.getFormat().setPattern("{username}*");

		ConflictResolution resolution = resolver.resolve(context, config);
		assertEquals(ConflictResolution.Action.ALLOW, resolution.getAction());
		assertEquals("Player*", resolution.getOverrideValue());
	}

	@Test
	void formatsDisplayNamesAndProviderIdsWhenAvailable() {
		ProviderOperations providerOperations = mock(ProviderOperations.class);
		when(providerOperations.displayProviderName("premium")).thenReturn("Premium");
		when(providerOperations.displayProviderName("cracked")).thenReturn("Offline");

		FormatUsernameConflictResolver resolver = new FormatUsernameConflictResolver(providerOperations);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("Player")
				.incomingLink(link("premium"))
				.existingLink(link("cracked"))
				.build();

		FormatUsernameConflictResolver.Config config = new FormatUsernameConflictResolver.Config();
		config.getFormat().setPattern("{username} [{incomingProvider}] ({incomingProviderId}->{existingProviderId})");

		ConflictResolution resolution = resolver.resolve(context, config);
		assertEquals("Player [Premium] (premium->cracked)", resolution.getOverrideValue());
	}

	private AccountProviderLink link(String providerId) {
		return AccountProviderLink.builder()
				.uniqueId(UUID.randomUUID())
				.providerId(providerId)
				.providerSubject("subject-" + providerId)
				.primaryLink(true)
				.build();
	}
}
