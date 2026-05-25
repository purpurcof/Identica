package me.whereareiam.identica.common.identity.session.recognition.eligibility.rule;

import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityContext;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityRuleDecision;
import me.whereareiam.identica.type.session.recognition.RecognitionAttemptKind;
import me.whereareiam.identica.type.session.recognition.RecognitionTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Recognition Enabled Eligibility Rule")
class RecognitionEnabledEligibilityRuleTest {
	@DisplayName("Blocks session recognition when recognition is disabled")
	@Test
	void blocksSessionRecognitionWhenRecognitionIsDisabled() {
		Settings settings = settings(false);
		Providers providers = providers(null);

		RecognitionEligibilityRuleDecision decision = new RecognitionEnabledEligibilityRule(() -> settings, () -> providers)
				.evaluate(context(RecognitionAttemptKind.SESSION_RECOGNITION));

		assertEquals(RecognitionEligibilityRuleDecision.Status.BLOCK, decision.getStatus());
	}

	@DisplayName("Provider override restores recognition when enabled")
	@Test
	void providerOverrideRestoresRecognitionWhenEnabled() {
		Settings settings = settings(false);
		Providers providers = providers(true);

		RecognitionEligibilityRuleDecision decision = new RecognitionEnabledEligibilityRule(() -> settings, () -> providers)
				.evaluate(context(RecognitionAttemptKind.SESSION_RECOGNITION));

		assertEquals(RecognitionEligibilityRuleDecision.Status.ABSTAIN, decision.getStatus());
	}

	private RecognitionEligibilityContext context(RecognitionAttemptKind attemptKind) {
		return RecognitionEligibilityContext.builder()
				.providerId("premium")
				.attemptKind(attemptKind)
				.trigger(RecognitionTrigger.AUTOMATIC)
				.build();
	}

	private Settings settings(boolean enabled) {
		Settings.Sessions.Recognition recognition = new Settings.Sessions.Recognition();
		recognition.setEnabled(enabled);
		recognition.setValidity(Duration.ofHours(12));

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setRecognition(recognition);

		Settings.Connection connection = new Settings.Connection();
		connection.setSessions(sessions);

		Settings settings = new Settings();
		settings.setConnection(connection);
		return settings;
	}

	private Providers providers(Boolean enabledOverride) {
		Providers.ProviderEntry provider = new Providers.ProviderEntry();
		provider.setId("premium");
		provider.getOverrides().getRecognition().setEnabled(enabledOverride);

		Providers providers = new Providers();
		providers.setProviders(List.of(provider));
		return providers;
	}
}
