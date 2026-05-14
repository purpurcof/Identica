package me.whereareiam.identica.common.identity.session.recognition.policy;

import me.whereareiam.identica.identity.session.recognition.policy.UntrustedIpRecognitionDecision.Outcome;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Untrusted IP Recognition Guard")
class UntrustedIpRecognitionGuardTest {
	@DisplayName("Matches an exact IPv4 entry")
	@Test
	void matchesExactIpv4Entry() {
		assertTrue(guard(settings(true, List.of("203.0.113.10")), providers())
				.isUntrustedIp("203.0.113.10"));
	}

	@DisplayName("Matches an IPv4 CIDR entry")
	@Test
	void matchesIpv4CidrEntry() {
		assertTrue(guard(settings(true, List.of("203.0.113.0/24")), providers())
				.isUntrustedIp("203.0.113.77"));
	}

	@DisplayName("Matches an IPv6 CIDR entry")
	@Test
	void matchesIpv6CidrEntry() {
		assertTrue(guard(settings(true, List.of("2001:db8::/32")), providers())
				.isUntrustedIp("2001:db8::42"));
	}

	@DisplayName("Does not block when the feature is disabled")
	@Test
	void disabledGuardDoesNotBlockRecognition() {
		assertFalse(guard(settings(false, List.of("203.0.113.10")), providers())
				.evaluateAutomaticRecognition("premium", "203.0.113.10", null)
				.isBlocked());
	}

	@DisplayName("Does not block when the client IP is missing")
	@Test
	void missingIpDoesNotBlockRecognition() {
		assertFalse(guard(settings(true, List.of("203.0.113.10")), providers())
				.evaluateAutomaticRecognition("premium", null, null)
				.isBlocked());
	}

	@DisplayName("Fails clearly when a configured entry is malformed")
	@Test
	void invalidEntryFailsClearly() {
		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> guard(settings(true, List.of("not-an-ip")), providers()).isUntrustedIp("203.0.113.10")
		);

		assertTrue(exception.getMessage().contains("settings.connection.sessions.recognition.untrustedIps.entries[0]"));
	}

	@DisplayName("Provider override restores automatic recognition")
	@Test
	void providerOverrideRestoresRecognition() {
		Providers providers = providers();
		provider(providers).getOverrides().setAllowRecognitionOnUntrustedIp(true);

        assertSame(Outcome.ALLOWED_PROVIDER_OVERRIDE, guard(settings(true, List.of("203.0.113.10")), providers)
                .evaluateAutomaticRecognition("premium", "203.0.113.10", null)
                .getOutcome());
	}

	@DisplayName("Explicit entrypoint selection bypasses blocking")
	@Test
	void explicitEntrypointSelectionBypassesBlocking() {
        assertSame(Outcome.ALLOWED_EXPLICIT_SELECTION, guard(settings(true, List.of("203.0.113.10")), providers())
                .evaluateAutomaticRecognition(
                        "premium",
                        "203.0.113.10",
                        ProviderContext.of("premium", null, "PlayerOne", ProviderOrigin.ENTRYPOINT)
                ).getOutcome());
	}

	@DisplayName("Explicit manual selection bypasses blocking")
	@Test
	void explicitManualSelectionBypassesBlocking() {
        assertSame(Outcome.ALLOWED_EXPLICIT_SELECTION, guard(settings(true, List.of("203.0.113.10")), providers())
                .evaluateAutomaticRecognition(
                        "premium",
                        "203.0.113.10",
                        ProviderContext.of("premium", null, "PlayerOne", ProviderOrigin.MANUAL)
                ).getOutcome());
	}

	@DisplayName("Valid configuration can be resolved eagerly without throwing")
	@Test
	void validConfigurationResolvesEagerly() {
		assertDoesNotThrow(() -> guard(settings(true, List.of("203.0.113.0/24")), providers())
				.isUntrustedIp("203.0.113.42"));
	}

	private UntrustedIpRecognitionGuard guard(Settings settings, Providers providers) {
		return new UntrustedIpRecognitionGuard(() -> settings, () -> providers);
	}

	private Settings settings(boolean enabled, List<String> entries) {
		Settings.Sessions.Recognition.UntrustedIps untrustedIps = new Settings.Sessions.Recognition.UntrustedIps();
		untrustedIps.setEnabled(enabled);
		untrustedIps.setEntries(entries);

		Settings.Sessions.Recognition recognition = new Settings.Sessions.Recognition();
		recognition.setUntrustedIps(untrustedIps);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setRecognition(recognition);

		Settings.Connection connection = new Settings.Connection();
		connection.setSessions(sessions);

		Settings settings = new Settings();
		settings.setConnection(connection);
		return settings;
	}

	private Providers providers() {
		Providers providers = new Providers();
		Providers.ProviderEntry premium = new Providers.ProviderEntry();
		premium.setId("premium");
		Providers.ProviderEntry credential = new Providers.ProviderEntry();
		credential.setId("credential");
		providers.setProviders(List.of(premium, credential));
		return providers;
	}

	private Providers.ProviderEntry provider(Providers providers) {
		return providers.getProviders().stream()
				.filter(entry -> entry.getId().equals("premium"))
				.findFirst()
				.orElseThrow();
	}
}
