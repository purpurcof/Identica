package me.whereareiam.identica.common.config.defaults;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.type.Format;
import me.whereareiam.identica.common.config.defaults.provider.ProvidersDefaults;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Providers Defaults")
class ProvidersDefaultsTest {
	@DisplayName("Provider session defaults stay absent until declared")
	@Test
	void providerSessionDefaultsStayAbsentUntilDeclared() {
		Providers providers = new ProvidersDefaults().supply(new Providers());

		assertNull(provider(providers, "premium").getSession());
		assertNull(provider(providers, "credential").getSession());
	}

	@DisplayName("Generated providers file keeps optional subtrees absent by default")
	@Test
	void generatedProvidersFileKeepsOptionalSubtreesAbsentByDefault(@TempDir Path tempDir) throws Exception {
		Path providersPath = tempDir.resolve("providers.yml");
		Providers providers = yaml().update(providersPath, Providers.class);

		Providers.ProviderEntry premium = provider(providers, "premium");
		assertEquals("premium", premium.getId());
		assertTrue(premium.isEnabled());
		assertEquals(100, premium.getPriority());
		assertEquals(List.of("premium.arcadeya.com"), premium.getEntrypoints());

		String generated = Files.readString(providersPath);
		assertTrue(generated.contains("providers:"));
		assertTrue(generated.contains("entrypoints:"));
		assertFalse(generated.contains("session:"), generated);
		assertFalse(generated.contains("verification:"), generated);

		assertNull(premium.getSession());
		assertNull(premium.getVerification());
	}

	@DisplayName("Declared verification merges provider defaults")
	@Test
	void declaredVerificationMergesProviderDefaults(@TempDir Path tempDir) throws Exception {
		Path providersPath = tempDir.resolve("providers.yml");
		Files.writeString(providersPath, """
				providers:
				  - id: premium
				    verification:
				      enabled: false
				""");

		Providers providers = yaml().update(providersPath, Providers.class);
		Providers.ProviderEntry.Verification verification = provider(providers, "premium").getVerification();

		assertNotNull(verification);
		assertFalse(verification.isEnabled());
		assertFalse(verification.isRequired());
		assertEquals(UnavailableSelectionPolicy.KEEP_LOCKED, verification.getUnavailableSelectionPolicy());
		assertEquals(1, verification.getMethods().size());
		assertEquals("totp", verification.getMethods().getFirst().getId());
		assertTrue(verification.getMethods().getFirst().isEnabled());
		assertEquals(100, verification.getMethods().getFirst().getPriority());
	}

	@DisplayName("Declared session stays minimal when no provider session fields are set")
	@Test
	void declaredSessionStaysMinimalWhenNoFieldsAreSet(@TempDir Path tempDir) throws Exception {
		Path providersPath = tempDir.resolve("providers.yml");
		Files.writeString(providersPath, """
				providers:
				  - id: premium
				    session: {}
				""");

		Providers providers = yaml().update(providersPath, Providers.class);
		Providers.ProviderEntry.Session session = provider(providers, "premium").getSession();

		assertNotNull(session);
		assertNull(session.getConcurrencyPolicy());
	}

	private Configura yaml() {
		return Config.builder()
				.format(Format.YAML)
				.defaults(ProvidersDefaults.class)
				.build();
	}

	private Providers.ProviderEntry provider(Providers providers, String id) {
		return providers.getProviders().stream()
				.filter(entry -> entry.getId().equals(id))
				.findFirst()
				.orElseThrow();
	}
}
