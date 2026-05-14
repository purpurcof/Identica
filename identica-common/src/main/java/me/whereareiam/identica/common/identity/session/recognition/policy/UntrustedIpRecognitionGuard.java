package me.whereareiam.identica.common.identity.session.recognition.policy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.identity.session.recognition.policy.matcher.UntrustedIpMatcher;
import me.whereareiam.identica.common.identity.session.recognition.policy.matcher.type.CidrMatcher;
import me.whereareiam.identica.common.identity.session.recognition.policy.matcher.type.ExactIpMatcher;
import me.whereareiam.identica.identity.session.recognition.policy.UntrustedIpRecognitionDecision;
import me.whereareiam.identica.identity.session.recognition.policy.UntrustedIpRecognitionDecision.Outcome;
import me.whereareiam.identica.identity.session.recognition.policy.UntrustedIpRecognitionPolicy;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UntrustedIpRecognitionGuard implements UntrustedIpRecognitionPolicy {
	private final Provider<Settings> settingsProvider;
	private final Provider<Providers> providersProvider;

	@Override
	public @NotNull UntrustedIpRecognitionDecision evaluateAutomaticRecognition(
			@Nullable String providerId,
			@Nullable String clientIp,
			@Nullable ProviderContext selectedProvider
	) {
		Settings.Sessions.Recognition.UntrustedIps untrustedIps = settingsProvider.get()
				.getConnection()
				.getSessions()
				.getRecognition()
				.getUntrustedIps();

		if (!untrustedIps.isEnabled()) return decision(Outcome.ALLOWED_DISABLED);
		if (allowsRecognitionOverride(providerId)) return decision(Outcome.ALLOWED_PROVIDER_OVERRIDE);
		if (isExplicitSelection(selectedProvider, providerId))
			return decision(Outcome.ALLOWED_EXPLICIT_SELECTION);

		InetAddress clientAddress = parseClientAddress(clientIp);
		if (clientAddress == null) return decision(Outcome.ALLOWED_MISSING_IP);

		List<String> entries = untrustedIps.getEntries();
		for (int i = 0; i < entries.size(); i++) {
			UntrustedIpMatcher matcher = parseEntry(entries.get(i), i);
			if (matcher.matches(clientAddress)) return decision(Outcome.BLOCKED_UNTRUSTED_IP);
		}

		return decision(Outcome.ALLOWED_IP_NOT_MATCHED);
	}

	@Override
	public boolean isUntrustedIp(@Nullable String clientIp) {
		return evaluateAutomaticRecognition(null, clientIp, null).isBlocked();
	}

	private boolean allowsRecognitionOverride(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return false;

		for (Providers.ProviderEntry entry : providersProvider.get().getProviders()) {
			if (entry == null) continue;
			if (!entry.getId().equalsIgnoreCase(providerId)) continue;
			return entry.getOverrides().isAllowRecognitionOnUntrustedIp();
		}

		return false;
	}

	private boolean isExplicitSelection(@Nullable ProviderContext selectedProvider, @Nullable String providerId) {
		if (selectedProvider == null || providerId == null || providerId.isBlank()) return false;
		if (selectedProvider.getProviderId() == null || !selectedProvider.getProviderId().equalsIgnoreCase(providerId))
			return false;

		ProviderOrigin source = selectedProvider.getSource();
		return source == ProviderOrigin.ENTRYPOINT || source == ProviderOrigin.MANUAL;
	}

	private @NotNull UntrustedIpRecognitionDecision decision(@NotNull Outcome outcome) {
		return new UntrustedIpRecognitionDecision(outcome);
	}

	private @NotNull UntrustedIpMatcher parseEntry(@Nullable String entry, int index) {
		if (entry == null || entry.isBlank()) throw invalidEntry(index, entry, "entry must not be blank");

		String trimmed = entry.trim();
		if (trimmed.contains("/")) return parseCidr(trimmed, index);

		InetAddress literal = parseLiteral(trimmed, index, "exact IP");
		return new ExactIpMatcher(literal);
	}

	private @NotNull UntrustedIpMatcher parseCidr(@NotNull String entry, int index) {
		String[] parts = entry.split("/", -1);
		if (parts.length != 2) throw invalidEntry(index, entry, "CIDR entry must contain exactly one '/' separator");

		InetAddress base = parseLiteral(parts[0], index, "CIDR base");
		int prefix;
		try {
			prefix = Integer.parseInt(parts[1]);
		} catch (NumberFormatException exception) {
			throw invalidEntry(index, entry, "CIDR prefix must be a number");
		}

		int maxPrefix = base.getAddress().length * 8;
		if (prefix < 0 || prefix > maxPrefix)
			throw invalidEntry(index, entry, "CIDR prefix must be between 0 and " + maxPrefix);

		return new CidrMatcher(base, prefix);
	}

	private @NotNull InetAddress parseLiteral(
			@Nullable String value,
			int index,
			@NotNull String label
	) {
		String trimmed = value == null ? "" : value.trim();
		if (!looksLikeIpLiteral(trimmed)) throw invalidEntry(index, value, label + " must be an IP literal");

		try {
			return InetAddress.getByName(trimmed);
		} catch (UnknownHostException exception) {
			throw invalidEntry(index, value, label + " must be a valid IP literal");
		}
	}

	private @Nullable InetAddress parseClientAddress(@Nullable String clientIp) {
		if (clientIp == null || clientIp.isBlank()) return null;

		String trimmed = clientIp.trim();
		if (!looksLikeIpLiteral(trimmed)) return null;

		try {
			return InetAddress.getByName(trimmed);
		} catch (UnknownHostException exception) {
			return null;
		}
	}

	private boolean looksLikeIpLiteral(@NotNull String value) {
		if (value.isBlank()) return false;

		if (value.indexOf(':') >= 0) return value.chars().allMatch(character -> isHexDigit(character)
				|| character == ':'
				|| character == '.');

		String[] parts = value.split("\\.", -1);
		if (parts.length != 4) return false;

		for (String part : parts) {
			if (part.isBlank() || part.length() > 3) return false;
			for (int i = 0; i < part.length(); i++) {
				if (!Character.isDigit(part.charAt(i))) return false;
			}

			int octet = Integer.parseInt(part);
			if (octet < 0 || octet > 255) return false;
		}

		return true;
	}

	private boolean isHexDigit(int character) {
		char lowered = Character.toLowerCase((char) character);
		return (lowered >= '0' && lowered <= '9') || (lowered >= 'a' && lowered <= 'f');
	}

	private @NotNull IllegalStateException invalidEntry(int index, @Nullable String entry, @NotNull String reason) {
		return new IllegalStateException(
				"settings.connection.sessions.recognition.untrustedIps.entries[" + index + "]="
						+ entry
						+ " is invalid: "
						+ reason
		);
	}
}
