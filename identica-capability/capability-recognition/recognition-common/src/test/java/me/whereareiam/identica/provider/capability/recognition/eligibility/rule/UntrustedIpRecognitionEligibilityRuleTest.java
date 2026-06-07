package me.whereareiam.identica.provider.capability.recognition.eligibility.rule;

import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.provider.capability.recognition.config.RecognitionSettings;
import me.whereareiam.identica.provider.capability.recognition.config.provider.RecognitionCapabilities;
import me.whereareiam.identica.provider.capability.recognition.config.provider.RecognitionProvidersProvider;
import me.whereareiam.identica.provider.capability.recognition.model.eligibility.RecognitionEligibilityContext;
import me.whereareiam.identica.provider.capability.recognition.model.eligibility.RecognitionEligibilityRuleDecision;
import me.whereareiam.identica.provider.capability.recognition.type.RecognitionAttemptKind;
import me.whereareiam.identica.provider.capability.recognition.type.RecognitionTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Untrusted IP Recognition Eligibility Rule")
class UntrustedIpRecognitionEligibilityRuleTest {
	@DisplayName("Matches exact IPv4 entries")
	@Test
	void matchesExactIpv4Entries() {
		RecognitionEligibilityRuleDecision decision = rule(settings(List.of("203.0.113.10")), providersProvider(false))
				.evaluate(context("203.0.113.10"));

		assertEquals(RecognitionEligibilityRuleDecision.Status.BLOCK, decision.getStatus());
	}

	@DisplayName("Provider recognition settings allow recognition on untrusted IPs")
	@Test
	void providerRecognitionSettingsAllowRecognitionOnUntrustedIps() {
		RecognitionSettings settings = settings(List.of("203.0.113.10"));
		RecognitionEligibilityRuleDecision decision = rule(settings, providersProvider(true))
				.evaluate(context("203.0.113.10"));

		assertEquals(RecognitionEligibilityRuleDecision.Status.ALLOW, decision.getStatus());
	}

	@DisplayName("Invalid entries fail with the eligibility config path")
	@Test
	void invalidEntriesFailWithEligibilityConfigPath() {
		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> rule(settings(List.of("not-an-ip")), providersProvider(false)).evaluate(context("203.0.113.10"))
		);

		assertTrue(exception.getMessage().contains("providers.capabilities.recognition.settings.eligibility.untrustedIps.entries[0]"));
	}

	private UntrustedIpRecognitionEligibilityRule rule(
			RecognitionSettings settings,
			RecognitionProvidersProvider providersProvider
	) {
		return new UntrustedIpRecognitionEligibilityRule(() -> settings, providersProvider);
	}

	private RecognitionEligibilityContext context(String ip) {
		return RecognitionEligibilityContext.builder()
				.providerId("premium")
				.clientIp(ip)
				.attemptKind(RecognitionAttemptKind.SESSION_RECOGNITION)
				.trigger(RecognitionTrigger.AUTOMATIC)
				.build();
	}

	private RecognitionSettings settings(List<String> entries) {
		RecognitionSettings.Eligibility.UntrustedIps untrustedIps = new RecognitionSettings.Eligibility.UntrustedIps();
		untrustedIps.setEnabled(true);
		untrustedIps.setEntries(entries);

		RecognitionSettings.Eligibility eligibility = new RecognitionSettings.Eligibility();
		eligibility.setUntrustedIps(untrustedIps);

		RecognitionSettings settings = new RecognitionSettings();
		settings.setEligibility(eligibility);
		return settings;
	}

	private RecognitionProvidersProvider providersProvider(boolean allowOnUntrustedIp) {
		RecognitionProvidersProvider provider = mock(RecognitionProvidersProvider.class);
		Providers providers = new Providers();
		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		RecognitionCapabilities capabilities = new RecognitionCapabilities();
		RecognitionCapabilities.Recognition recognition = new RecognitionCapabilities.Recognition();
		recognition.setAllowOnUntrustedIps(allowOnUntrustedIp);
		capabilities.setRecognition(recognition);
		premium.setDeclaredCapabilityIds(capabilities);
		providers.setProviders(List.of(premium));
		when(provider.get()).thenReturn(providers);
		return provider;
	}
}
