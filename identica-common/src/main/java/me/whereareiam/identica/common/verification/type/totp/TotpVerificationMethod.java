package me.whereareiam.identica.common.verification.type.totp;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.PendingVerificationEnrollment;
import me.whereareiam.identica.verification.VerificationMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static me.whereareiam.identica.common.verification.RecoveryCodeGenerator.generateCodes;

@Singleton
public class TotpVerificationMethod implements VerificationMethod {
	private static final int SECRET_BYTES = 20;

	@Override
	public @NotNull String id() {
		return "totp";
	}

	@Override
	public @NotNull String displayName(@NotNull Verification config) {
		String displayName = config.getTotp().getDisplayName();
		return displayName.isBlank() ? id() : displayName;
	}

	@Override
	public @NotNull PendingVerificationEnrollment beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull Verification config
	) {
		Verification.Totp totp = config.getTotp();
		String resolvedProviderId = providerId == null || providerId.isBlank() ? "unknown" : providerId;
		String secret = TotpCodec.generateSecret(SECRET_BYTES);
		String label = totp.getLabelFormat()
				.replace("{player}", username)
				.replace("{providerId}", resolvedProviderId);

		String uri = TotpCodec.buildOtpAuthUri(
				totp.getIssuer(),
				label,
				secret,
				totp.getDigits(),
				totp.periodSeconds()
		);

		return PendingVerificationEnrollment.builder()
				.uniqueId(uniqueId)
				.username(username)
				.providerId(resolvedProviderId)
				.methodId(id())
				.payload(secret)
				.secret(secret)
				.otpauthUri(uri)
				.createdAt(System.currentTimeMillis())
				.build();
	}

	@Override
	public boolean verifyEnrollment(
			@NotNull PendingVerificationEnrollment pending,
			@NotNull String input,
			@NotNull Verification config
	) {
		return verifyChallenge(pending.getPayload(), input, config);
	}

	@Override
	public boolean verifyChallenge(
			@NotNull String payload,
			@NotNull String input,
			@NotNull Verification config
	) {
		Verification.Totp totp = config.getTotp();
		return TotpCodec.verify(
				payload,
				input,
				totp.getDigits(),
				totp.periodSeconds(),
				totp.getAllowedPastWindows(),
				totp.getAllowedFutureWindows()
		);
	}

	@Override
	public @NotNull List<String> generateRecoveryCodes(@NotNull Verification config) {
		Verification.RecoveryCodes recoveryCodes = config.getTotp().getRecoveryCodes();
		return generateCodes(
				recoveryCodes.getAmount(),
				recoveryCodes.getLength(),
				recoveryCodes.getGroupSize()
		);
	}
}
