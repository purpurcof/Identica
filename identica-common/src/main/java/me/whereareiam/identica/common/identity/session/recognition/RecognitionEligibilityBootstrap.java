package me.whereareiam.identica.common.identity.session.recognition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.rule.ExplicitSelectionRecognitionEligibilityRule;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.rule.RecognitionEnabledEligibilityRule;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.rule.UntrustedIpRecognitionEligibilityRule;
import me.whereareiam.identica.identity.session.recognition.eligibility.RecognitionEligibilityRegistry;

@Singleton
public class RecognitionEligibilityBootstrap {
	@Inject
	public RecognitionEligibilityBootstrap(
			RecognitionEligibilityRegistry registry,
			RecognitionEnabledEligibilityRule recognitionEnabledEligibilityRule,
			ExplicitSelectionRecognitionEligibilityRule explicitSelectionRecognitionEligibilityRule,
			UntrustedIpRecognitionEligibilityRule untrustedIpRecognitionEligibilityRule
	) {
		registry.register(recognitionEnabledEligibilityRule);
		registry.register(explicitSelectionRecognitionEligibilityRule);
		registry.register(untrustedIpRecognitionEligibilityRule);
	}
}
