package me.whereareiam.identica.common.identity.session.recognition.eligibility.rule;

import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityContext;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityRuleDecision;
import me.whereareiam.identica.type.session.recognition.RecognitionAttemptKind;
import me.whereareiam.identica.type.session.recognition.RecognitionTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Untrusted IP Recognition Eligibility Rule")
class UntrustedIpRecognitionEligibilityRuleTest {
	@DisplayName("Matches exact IPv4 entries")
	@Test
	void matchesExactIpv4Entries() {
		RecognitionEligibilityRuleDecision decision = rule(settings(true, List.of("203.0.113.10")), providers(false))
				.evaluate(context("203.0.113.10"));

		assertEquals(RecognitionEligibilityRuleDecision.Status.BLOCK, decision.getStatus());
	}

	@DisplayName("Provider override allows recognition on untrusted IPs")
	@Test
	void providerOverrideAllowsRecognitionOnUntrustedIps() {
		RecognitionEligibilityRuleDecision decision = rule(settings(true, List.of("203.0.113.10")), providers(true))
				.evaluate(context("203.0.113.10"));

		assertEquals(RecognitionEligibilityRuleDecision.Status.ALLOW, decision.getStatus());
	}

	@DisplayName("Invalid entries fail with the eligibility config path")
	@Test
	void invalidEntriesFailWithEligibilityConfigPath() {
		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> rule(settings(true, List.of("not-an-ip")), providers(false)).evaluate(context("203.0.113.10"))
		);

		assertTrue(exception.getMessage().contains("settings.connection.sessions.recognition.eligibility.untrustedIps.entries[0]"));
	}

	private UntrustedIpRecognitionEligibilityRule rule(Settings settings, Providers providers) {
		return new UntrustedIpRecognitionEligibilityRule(() -> settings, () -> providers);
	}

	private RecognitionEligibilityContext context(String ip) {
		return RecognitionEligibilityContext.builder()
				.providerId("premium")
				.clientIp(ip)
				.attemptKind(RecognitionAttemptKind.SESSION_RECOGNITION)
				.trigger(RecognitionTrigger.AUTOMATIC)
				.build();
	}

	private Settings settings(boolean enabled, List<String> entries) {
		Settings.Sessions.Recognition.Eligibility.UntrustedIps untrustedIps =
				new Settings.Sessions.Recognition.Eligibility.UntrustedIps();
		untrustedIps.setEnabled(enabled);
		untrustedIps.setEntries(entries);

		Settings.Sessions.Recognition.Eligibility eligibility = new Settings.Sessions.Recognition.Eligibility();
		eligibility.setUntrustedIps(untrustedIps);

		Settings.Sessions.Recognition recognition = new Settings.Sessions.Recognition();
		recognition.setEligibility(eligibility);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setRecognition(recognition);

		Settings.Connection connection = new Settings.Connection();
		connection.setSessions(sessions);

		Settings settings = new Settings();
		settings.setConnection(connection);
		return settings;
	}

	private Providers providers(boolean allowOnUntrustedIp) {
		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		premium.getOverrides().getRecognition().getEligibility().setAllowOnUntrustedIp(allowOnUntrustedIp);

		Providers providers = new Providers();
		providers.setProviders(List.of(premium));
		return providers;
	}
}
