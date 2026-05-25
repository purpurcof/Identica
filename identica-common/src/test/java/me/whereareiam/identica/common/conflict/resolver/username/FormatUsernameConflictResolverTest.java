package me.whereareiam.identica.common.conflict.resolver.username;

import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.provider.ProviderOperations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Format-Username Conflict Resolver")
class FormatUsernameConflictResolverTest {
	@DisplayName("Formats the username when a custom pattern is provided")
	@Test
	void formatsUsernameWhenPatternProvided() {
		ProviderOperations providerOperations = mock(ProviderOperations.class);
		FormatUsernameConflictResolver resolver = new FormatUsernameConflictResolver(providerOperations);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("Player")
				.incomingLink(link("premium"))
				.existingLink(link("credential"))
				.build();

		FormatUsernameConflictResolver.Config config = new FormatUsernameConflictResolver.Config();
		config.getFormat().setPattern("{username}*");

		ConflictResolution resolution = resolver.resolve(context, config);
		assertEquals(ConflictResolution.Action.ALLOW, resolution.getAction());
		assertEquals("Player*", resolution.getOverrideValue());
	}

	@DisplayName("Expands provider names and IDs inside username format patterns")
	@Test
	void formatsDisplayNamesAndProviderIdsWhenAvailable() {
		ProviderOperations providerOperations = mock(ProviderOperations.class);
		when(providerOperations.displayProviderName("premium")).thenReturn("Premium");
		when(providerOperations.displayProviderName("credential")).thenReturn("Credential");

		FormatUsernameConflictResolver resolver = new FormatUsernameConflictResolver(providerOperations);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("Player")
				.incomingLink(link("premium"))
				.existingLink(link("credential"))
				.build();

		FormatUsernameConflictResolver.Config config = new FormatUsernameConflictResolver.Config();
		config.getFormat().setPattern("{username} [{incomingProvider}] ({incomingProviderId}->{existingProviderId})");

		ConflictResolution resolution = resolver.resolve(context, config);
		assertEquals("Player [Premium] (premium->credential)", resolution.getOverrideValue());
	}

	@DisplayName("Cuts placeholder values when max symbol count is provided")
	@Test
	void truncatesProviderNamePlaceholdersWhenMaxSymbolCountProvided() {
		ProviderOperations providerOperations = mock(ProviderOperations.class);
		when(providerOperations.displayProviderName("premium")).thenReturn("PremiumPlus");
		when(providerOperations.displayProviderName("credential")).thenReturn("Credential");

		FormatUsernameConflictResolver resolver = new FormatUsernameConflictResolver(providerOperations);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("PlayerOne")
				.incomingLink(link("premium"))
				.existingLink(link("credential"))
				.build();

		FormatUsernameConflictResolver.Config config = new FormatUsernameConflictResolver.Config();
		config.getFormat().setPattern("{username}_{incomingProvider:7}_{incomingProviderId}");

		ConflictResolution resolution = resolver.resolve(context, config);
		assertEquals("PlayerOne_Premium_premium", resolution.getOverrideValue());
	}

	@DisplayName("Keeps random placeholder digit counts working")
	@Test
	void keepsRandomPlaceholderDigitCountsWorking() {
		ProviderOperations providerOperations = mock(ProviderOperations.class);
		FormatUsernameConflictResolver resolver = new FormatUsernameConflictResolver(providerOperations);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.candidate("Player")
				.build();

		FormatUsernameConflictResolver.Config config = new FormatUsernameConflictResolver.Config();
		config.getFormat().setPattern("{username}_{random:4}");

		ConflictResolution resolution = resolver.resolve(context, config);
		assertTrue(resolution.getOverrideValue().matches("Player_\\d{4}"));
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
