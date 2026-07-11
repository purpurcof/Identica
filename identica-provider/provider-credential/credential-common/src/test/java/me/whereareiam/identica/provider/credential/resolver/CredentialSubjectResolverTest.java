package me.whereareiam.identica.provider.credential.resolver;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.provider.subject.SubjectResolution;
import me.whereareiam.identica.provider.subject.SubjectResolveContext;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Credential Subject Resolver")
class CredentialSubjectResolverTest {
	private final CredentialSubjectResolver resolver = new CredentialSubjectResolver();

	@DisplayName("Derives the credential provider subject from the username")
	@Test
	void resolvesCredentialSubjectFromUsername() {
		SubjectResolution resolution = resolver.resolve(SubjectResolveContext.builder()
				.identity(new ConnectionIdentity("PlayerOne", "127.0.0.1"))
				.build());

		assertNotNull(resolution);
		assertEquals(CredentialConstants.PROVIDER_ID, resolution.getProviderId());
		assertEquals(UniqueIdGenerator.offlinePlayerUniqueId("PlayerOne").toString(), resolution.getProviderSubject());
	}

	@DisplayName("Rejects blank usernames")
	@Test
	void doesNotSupportBlankUsernames() {
		assertFalse(resolver.supports(SubjectResolveContext.builder()
				.identity(new ConnectionIdentity("", "127.0.0.1"))
				.build()));
		assertNull(resolver.resolve(SubjectResolveContext.builder()
				.identity(new ConnectionIdentity("", "127.0.0.1"))
				.build()));
	}
}
