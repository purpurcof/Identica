package me.whereareiam.identica.common.verification.type.totp;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.verification.RecoveryCodeGenerator;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentContext;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.model.verification.interaction.SavedVerificationInteraction;
import me.whereareiam.identica.model.verification.process.VerificationProcessDisplay;
import me.whereareiam.identica.type.verification.VerificationEnrollmentStatus;
import me.whereareiam.identica.verification.VerificationEnrollmentProcess;
import me.whereareiam.identica.verification.VerificationInteraction;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class TotpEnrollmentProcess implements VerificationEnrollmentProcess<TotpEnrollmentState> {
	private static final int SECRET_BYTES = 20;

	private final Provider<Verification> verificationProvider;

	@Inject
	public TotpEnrollmentProcess(Provider<Verification> verificationProvider) {
		this.verificationProvider = verificationProvider;
	}

	@Override
	public @NotNull Class<TotpEnrollmentState> stateType() {
		return TotpEnrollmentState.class;
	}

	@Override
	public @NotNull VerificationEnrollmentResult<TotpEnrollmentState> start(
			@NotNull VerificationEnrollmentContext<TotpEnrollmentState> context
	) {
		Verification.Totp totp = verificationProvider.get().getTotp();
		String providerId = context.getProviderId() == null || context.getProviderId().isBlank()
				? "unknown"
				: context.getProviderId();
		String secret = TotpCodec.generateSecret(SECRET_BYTES);
		String label = totp.getLabelFormat()
				.replace("{player}", context.getUsername())
				.replace("{providerId}", providerId);
		String uri = TotpCodec.buildOtpAuthUri(
				totp.getIssuer(),
				label,
				secret,
				totp.getDigits(),
				totp.periodSeconds()
		);
		TotpEnrollmentState state = TotpEnrollmentState.builder()
				.secret(secret)
				.uri(uri)
				.build();

		return result(VerificationEnrollmentStatus.STARTED, context, state, display(secret, uri), null);
	}

	@Override
	public @NotNull VerificationEnrollmentResult<TotpEnrollmentState> submit(
			@NotNull VerificationEnrollmentContext<TotpEnrollmentState> context,
			@NotNull TotpEnrollmentState state,
			@NotNull VerificationInteraction interaction
	) {
		if (state.isVerified()) {
			if (interaction instanceof SavedVerificationInteraction) {
				return result(
						VerificationEnrollmentStatus.ACTIVATED,
						context,
						state,
						null,
						state.getRecoveryCodes()
				);
			}

			return result(VerificationEnrollmentStatus.WAITING, context, state, null, state.getRecoveryCodes());
		}

		if (!(interaction instanceof CodeVerificationInteraction code))
			return result(VerificationEnrollmentStatus.WAITING, context, state, display(state.getSecret(), state.getUri()), null);

		if (!verify(state.getSecret(), code.getCode()))
			return result(VerificationEnrollmentStatus.INVALID, context, state, display(state.getSecret(), state.getUri()), null);

		List<String> recoveryCodes = recoveryCodes();
		state.setVerified(true);
		state.setRecoveryCodes(recoveryCodes);
		return result(VerificationEnrollmentStatus.WAITING, context, state, null, recoveryCodes);
	}

	private boolean verify(@NotNull String secret, @NotNull String code) {
		Verification.Totp totp = verificationProvider.get().getTotp();
		return TotpCodec.verify(
				secret,
				code,
				totp.getDigits(),
				totp.periodSeconds(),
				totp.getAllowedPastWindows(),
				totp.getAllowedFutureWindows()
		);
	}

	private @NotNull List<String> recoveryCodes() {
		Verification.RecoveryCodes recoveryCodes = verificationProvider.get().getTotp().getRecoveryCodes();
		if (!recoveryCodes.isEnabled()) return List.of();
		return RecoveryCodeGenerator.generateCodes(
				recoveryCodes.getAmount(),
				recoveryCodes.getLength(),
				recoveryCodes.getGroupSize()
		);
	}

	private VerificationProcessDisplay display(@NotNull String secret, @NotNull String uri) {
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("secret", secret);
		placeholders.put("uri", uri);
		placeholders.put("otpauthUri", uri);
		placeholders.put("uriEncoded", URLEncoder.encode(uri, StandardCharsets.UTF_8));
		return VerificationProcessDisplay.builder()
				.placeholders(placeholders)
				.build();
	}

	private VerificationEnrollmentResult<TotpEnrollmentState> result(
			VerificationEnrollmentStatus status,
			VerificationEnrollmentContext<TotpEnrollmentState> context,
			TotpEnrollmentState state,
			VerificationProcessDisplay display,
			List<String> recoveryCodes
	) {
		return VerificationEnrollmentResult.<TotpEnrollmentState>builder()
				.status(status)
				.enrollmentId(context.getEnrollmentId())
				.methodId(context.getMethodId())
				.providerId(context.getProviderId())
				.state(state)
				.display(display)
				.methodData(display != null ? display.getPlaceholders() : null)
				.recoveryCodes(recoveryCodes)
				.build();
	}
}
