package me.whereareiam.identica.common.identity.session.recognition;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionService;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionStore;
import me.whereareiam.identica.identity.session.recognition.eligibility.RecognitionEligibilityService;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.session.SessionRecognitionSnapshot;
import me.whereareiam.identica.model.session.recognition.eligibility.RecognitionEligibilityContext;
import me.whereareiam.identica.type.session.recognition.RecognitionAttemptKind;
import me.whereareiam.identica.type.session.recognition.RecognitionSignal;
import me.whereareiam.identica.type.session.recognition.RecognitionTrigger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultSessionRecognitionService implements SessionRecognitionService {
	private final Provider<Settings> settingsProvider;
	private final Provider<Providers> providersProvider;
	private final SessionRecognitionStore sessionRecognitionStore;
	private final RecognitionEligibilityService recognitionEligibilityService;

	@Override
	public boolean matches(
			@Nullable String providerId,
			@Nullable String providerSubject,
			@Nullable String providerUsername,
			@Nullable String ip,
			@Nullable ConnectionIdentity.Origin origin
	) {
		if (isBlank(providerId) || isBlank(providerSubject) || !isRecognitionEnabled(providerId))
			return false;
		if (!recognitionEligibilityService.evaluate(RecognitionEligibilityContext.builder()
				.providerId(providerId)
				.providerUsername(providerUsername)
				.clientIp(ip)
				.origin(origin)
				.attemptKind(RecognitionAttemptKind.SESSION_RECOGNITION)
				.trigger(RecognitionTrigger.AUTOMATIC)
				.build()).isAllowed())
			return false;

		Optional<SessionRecognitionSnapshot> storedOptional = sessionRecognitionStore.find(providerId.trim(), providerSubject.trim());
		if (storedOptional.isEmpty()) return false;

		SessionRecognitionSnapshot stored = storedOptional.get();
		for (RecognitionSignal signal : effectiveSignals(providerId)) {
			if (!matchesSignal(signal, stored, providerUsername, ip, origin))
				return false;
		}

		return true;
	}

	private boolean matchesSignal(
			@NotNull RecognitionSignal signal,
			@NotNull SessionRecognitionSnapshot stored,
			@Nullable String providerUsername,
			@Nullable String ip,
			@Nullable ConnectionIdentity.Origin origin
	) {
		return switch (signal) {
			case USERNAME -> matchesText(stored.getProviderUsername(), providerUsername);
			case IP -> matchesText(stored.getLastIp(), ip);
			case VIRTUAL_HOST -> matchesOrigin(stored, origin);
		};
	}

	private boolean matchesOrigin(
			@NotNull SessionRecognitionSnapshot stored,
			@Nullable ConnectionIdentity.Origin origin
	) {
		if (origin == null) return false;
		return matchesText(stored.getLastVirtualHost(), origin.getHost())
				&& equalsNullable(stored.getLastVirtualPort(), origin.getPort());
	}

	private boolean isRecognitionEnabled(@NotNull String providerId) {
		Providers.ProviderEntry provider = findProvider(providerId);
		Providers.ProviderEntry.Overrides.Recognition overrides = provider != null
				? provider.getOverrides().getRecognition()
				: null;

		Boolean override = overrides != null ? overrides.getEnabled() : null;
		if (override != null) return override;

		return settingsProvider.get().getSessions().getRecognition().isEnabled();
	}

	private @NotNull Set<RecognitionSignal> effectiveSignals(@NotNull String providerId) {
		LinkedHashSet<RecognitionSignal> resolved = new LinkedHashSet<>();
		Providers.ProviderEntry provider = findProvider(providerId);
		List<RecognitionSignal> overrides = provider != null
				? provider.getOverrides().getRecognition().getSignals()
				: List.of();

		if (!overrides.isEmpty()) {
			resolved.addAll(overrides);
			return resolved;
		}

		List<RecognitionSignal> defaults = settingsProvider.get()
				.getSessions()
				.getRecognition()
				.getDefaultSignals();

        resolved.addAll(defaults);
		return resolved;
	}

	private @Nullable Providers.ProviderEntry findProvider(@Nullable String rawId) {
		if (isBlank(rawId)) return null;

		for (Providers.ProviderEntry provider : providersProvider.get().getProviders()) {
			if (provider == null || isBlank(provider.getId())) continue;
			if (provider.getId().trim().equalsIgnoreCase(rawId.trim())) return provider;
		}
		return null;
	}

	private boolean matchesText(@Nullable String left, @Nullable String right) {
		if (isBlank(left) || isBlank(right)) return false;
		return left.trim().equalsIgnoreCase(right.trim());
	}

	private boolean equalsNullable(@Nullable Integer left, @Nullable Integer right) {
		return left != null && left.equals(right);
	}

	private boolean isBlank(@Nullable String value) {
		return value == null || value.isBlank();
	}
}
