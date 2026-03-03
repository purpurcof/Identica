package me.whereareiam.identica.common.conflict.resolver.username;

import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatUsernameConflictResolverTest {
	@Test
	void formatsUsernameWhenPatternProvided() {
		FormatUsernameConflictResolver resolver = new FormatUsernameConflictResolver();

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

	private AccountProviderLink link(String providerId) {
		return AccountProviderLink.builder()
				.uniqueId(UUID.randomUUID())
				.providerId(providerId)
				.providerSubject("subject-" + providerId)
				.primaryLink(true)
				.build();
	}
}
