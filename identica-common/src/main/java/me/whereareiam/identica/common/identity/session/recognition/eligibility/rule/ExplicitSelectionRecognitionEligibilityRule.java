package me.whereareiam.identica.common.identity.session.recognition.eligibility.rule;

import com.google.inject.Singleton;
import me.whereareiam.identica.identity.session.recognition.eligibility.RecognitionEligibilityRule;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityContext;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityRuleDecision;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import me.whereareiam.identica.type.session.recognition.RecognitionAttemptKind;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ExplicitSelectionRecognitionEligibilityRule implements RecognitionEligibilityRule {
	@Override
	public @NotNull String id() {
		return "explicit-selection";
	}

	@Override
	public int order() {
		return 50;
	}

	@Override
	public boolean supports(@NotNull RecognitionEligibilityContext context) {
		return context.getAttemptKind() == RecognitionAttemptKind.PROVIDER_HANDSHAKE_RECOGNITION;
	}

	@Override
	public @NotNull RecognitionEligibilityRuleDecision evaluate(@NotNull RecognitionEligibilityContext context) {
		ProviderContext provider = context.getSelectedProvider();
		String providerId = context.getProviderId();
		if (provider == null || providerId == null || providerId.isBlank())
			return RecognitionEligibilityRuleDecision.abstain();
		if (provider.getProviderId() == null || !provider.getProviderId().equalsIgnoreCase(providerId))
			return RecognitionEligibilityRuleDecision.abstain();

		ProviderOrigin source = provider.getSource();
		if (source == ProviderOrigin.ENTRYPOINT || source == ProviderOrigin.MANUAL)
			return RecognitionEligibilityRuleDecision.allow("explicit-provider-selection");

		return RecognitionEligibilityRuleDecision.abstain();
	}
}
