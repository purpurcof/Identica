package me.whereareiam.identica.common.identity.session.recognition;

import me.whereareiam.identica.common.identity.session.recognition.eligibility.DefaultRecognitionEligibilityRegistry;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.DefaultRecognitionEligibilityService;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.rule.RecognitionEnabledEligibilityRule;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.rule.UntrustedIpRecognitionEligibilityRule;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionStore;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.session.SessionRecognitionSnapshot;
import me.whereareiam.identica.type.session.recognition.RecognitionSignal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Session Recognition Service")
class DefaultSessionRecognitionServiceTest {
	@DisplayName("Blocks provider recognition on an untrusted IP even when the stored snapshot matches")
	@Test
	void blocksRecognitionOnUntrustedIp() {
		Settings settings = settings(List.of(RecognitionSignal.USERNAME, RecognitionSignal.IP), List.of("127.0.0.1"));
		Providers providers = providers(false);
		SessionRecognitionStore store = store(snapshot("127.0.0.1", null, null));

		DefaultSessionRecognitionService service = service(settings, providers, store);

		assertFalse(service.matches("credential", "subject-1", "whereareiam", "127.0.0.1", null));
	}

	@DisplayName("Allows provider recognition on an untrusted IP when the provider override is enabled")
	@Test
	void allowsRecognitionOnUntrustedIpWhenProviderOverrideEnabled() {
		Settings settings = settings(List.of(RecognitionSignal.USERNAME, RecognitionSignal.IP), List.of("127.0.0.1"));
		Providers providers = providers(true);
		SessionRecognitionStore store = store(snapshot("127.0.0.1", null, null));

		DefaultSessionRecognitionService service = service(settings, providers, store);

		assertTrue(service.matches("credential", "subject-1", "whereareiam", "127.0.0.1", null));
	}

	@DisplayName("Keeps trusted IP recognition working for matching snapshots")
	@Test
	void keepsTrustedIpRecognitionWorking() {
		Settings settings = settings(
				List.of(RecognitionSignal.USERNAME, RecognitionSignal.IP, RecognitionSignal.VIRTUAL_HOST),
				List.of("127.0.0.1")
		);
		Providers providers = providers(false);
		SessionRecognitionStore store = store(snapshot("203.0.113.10", "localhost", 25565));

		DefaultSessionRecognitionService service = service(settings, providers, store);

		assertTrue(service.matches(
				"credential",
				"subject-1",
				"whereareiam",
				"203.0.113.10",
				new ConnectionIdentity.Origin("localhost", 25565)
		));
	}

	private DefaultSessionRecognitionService service(
			Settings settings,
			Providers providers,
			SessionRecognitionStore store
	) {
		DefaultRecognitionEligibilityRegistry registry = new DefaultRecognitionEligibilityRegistry();
		registry.register(new RecognitionEnabledEligibilityRule(() -> settings, () -> providers));
		registry.register(new UntrustedIpRecognitionEligibilityRule(() -> settings, () -> providers));

		return new DefaultSessionRecognitionService(
				() -> settings,
				() -> providers,
				store,
				new DefaultRecognitionEligibilityService(registry)
		);
	}

	private SessionRecognitionStore store(SessionRecognitionSnapshot snapshot) {
		SessionRecognitionStore store = mock(SessionRecognitionStore.class);
		when(store.find(snapshot.getProviderId(), snapshot.getProviderSubject())).thenReturn(Optional.of(snapshot));
		return store;
	}

	private Settings settings(
			List<RecognitionSignal> signals,
			List<String> untrustedIps
	) {
		Settings.Sessions.Recognition.Eligibility.UntrustedIps untrusted =
				new Settings.Sessions.Recognition.Eligibility.UntrustedIps();
		untrusted.setEnabled(true);
		untrusted.setEntries(untrustedIps);

		Settings.Sessions.Recognition.Eligibility eligibility = new Settings.Sessions.Recognition.Eligibility();
		eligibility.setUntrustedIps(untrusted);

		Settings.Sessions.Recognition recognition = new Settings.Sessions.Recognition();
		recognition.setEnabled(true);
		recognition.setValidity(Duration.ofHours(12));
		recognition.setDefaultSignals(signals);
		recognition.setEligibility(eligibility);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setRecognition(recognition);

		Settings settings = new Settings();
		settings.setSessions(sessions);
		return settings;
	}

	private Providers providers(boolean allowRecognitionOnUntrustedIp) {
		Providers.ProviderEntry provider = new Providers.ProviderEntry();
		provider.setId("credential");
		provider.getOverrides().getRecognition().getEligibility().setAllowOnUntrustedIp(allowRecognitionOnUntrustedIp);

		Providers providers = new Providers();
		providers.setProviders(List.of(provider));
		return providers;
	}

	private SessionRecognitionSnapshot snapshot(
			String lastIp,
			String lastVirtualHost,
			Integer lastVirtualPort
	) {
		return SessionRecognitionSnapshot.builder()
				.providerId("credential")
				.providerSubject("subject-1")
				.providerUsername("whereareiam")
				.lastIp(lastIp)
				.lastVirtualHost(lastVirtualHost)
				.lastVirtualPort(lastVirtualPort)
				.capturedAt(System.currentTimeMillis())
				.build();
	}
}
