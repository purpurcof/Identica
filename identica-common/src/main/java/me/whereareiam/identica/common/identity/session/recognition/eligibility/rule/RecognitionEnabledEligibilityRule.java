package me.whereareiam.identica.common.identity.session.recognition.eligibility.rule;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.identity.session.recognition.eligibility.RecognitionEligibilityRule;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityContext;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityRuleDecision;
import me.whereareiam.identica.type.session.recognition.RecognitionAttemptKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RecognitionEnabledEligibilityRule implements RecognitionEligibilityRule {
	private final Provider<Settings> settingsProvider;
	private final Provider<Providers> providersProvider;

	@Override
	public @NotNull String id() {
		return "recognition-enabled";
	}

	@Override
	public int order() {
		return 0;
	}

	@Override
	public boolean supports(@NotNull RecognitionEligibilityContext context) {
		return context.getAttemptKind() == RecognitionAttemptKind.SESSION_RECOGNITION;
	}

	@Override
	public @NotNull RecognitionEligibilityRuleDecision evaluate(@NotNull RecognitionEligibilityContext context) {
		String providerId = context.getProviderId();
		if (providerId == null || providerId.isBlank())
			return RecognitionEligibilityRuleDecision.block("recognition-provider-missing");
		if (!isRecognitionEnabled(providerId))
			return RecognitionEligibilityRuleDecision.block("recognition-disabled");

		return RecognitionEligibilityRuleDecision.abstain();
	}

	private boolean isRecognitionEnabled(@NotNull String providerId) {
		Providers.ProviderEntry provider = findProvider(providerId);
		Providers.ProviderEntry.Session session = provider != null
				? provider.getSession()
				: null;
		Providers.ProviderEntry.Session.Recognition recognition = session != null
				? session.getRecognition()
				: null;

		Boolean override = recognition != null ? recognition.getEnabled() : null;
		if (override != null) return override;

		return settingsProvider.get().getSessions().getRecognition().isEnabled();
	}

	private @Nullable Providers.ProviderEntry findProvider(@Nullable String rawId) {
		if (rawId == null || rawId.isBlank()) return null;

		for (Providers.ProviderEntry provider : providersProvider.get().getProviders()) {
			if (provider == null || provider.getId().isBlank()) continue;
			if (provider.getId().trim().equalsIgnoreCase(rawId.trim())) return provider;
		}
		return null;
	}
}
