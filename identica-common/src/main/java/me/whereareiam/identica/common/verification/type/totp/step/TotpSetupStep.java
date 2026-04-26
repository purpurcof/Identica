package me.whereareiam.identica.common.verification.type.totp.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessDisplay;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransition;
import me.whereareiam.identica.verification.process.VerificationProcessStep;
import me.whereareiam.identica.common.verification.type.totp.TotpCodec;
import me.whereareiam.identica.common.verification.type.totp.state.TotpEnrollmentState;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class TotpSetupStep implements VerificationProcessStep<CodeVerificationInteraction, TotpEnrollmentState> {
	private static final int SECRET_BYTES = 20;

	private final Provider<Verification> verificationProvider;

	@Inject
	public TotpSetupStep(Provider<Verification> verificationProvider) {
		this.verificationProvider = verificationProvider;
	}

	@Override
	public @NotNull String id() {
		return "totp-setup";
	}

	@Override
	public @NotNull Class<CodeVerificationInteraction> interactionType() {
		return CodeVerificationInteraction.class;
	}

	@Override
	public @NotNull Class<TotpEnrollmentState> stateType() {
		return TotpEnrollmentState.class;
	}

	@Override
	public @NotNull VerificationProcessResult<TotpEnrollmentState> start(
			@NotNull VerificationProcessContext<TotpEnrollmentState> context
	) {
		Verification.Totp totp = verificationProvider.get().getTotp();
		String providerId = context.getProviderId() == null || context.getProviderId().isBlank()
				? "unknown"
				: context.getProviderId();
		String username = context.getUsername() == null || context.getUsername().isBlank()
				? "unknown"
				: context.getUsername();
		String secret = TotpCodec.generateSecret(SECRET_BYTES);
		String label = totp.getLabelFormat()
				.replace("{player}", username)
				.replace("{providerId}", providerId);
		String uri = TotpCodec.buildOtpAuthUri(
				totp.getIssuer(),
				label,
				secret,
				totp.getDigits(),
				totp.periodSeconds()
		);

		TotpEnrollmentState state = TotpEnrollmentState.builder()
				.stepId(id())
				.secret(secret)
				.uri(uri)
				.verified(false)
				.build();

		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("secret", secret);
		placeholders.put("uri", uri);
		placeholders.put("otpauthUri", uri);
		placeholders.put("uriEncoded", URLEncoder.encode(uri, StandardCharsets.UTF_8));

		return VerificationProcessResult.<TotpEnrollmentState>builder()
				.status(me.whereareiam.identica.type.verification.VerificationProcessStatus.WAITING)
				.state(state)
				.display(VerificationProcessDisplay.builder()
						.placeholders(placeholders)
						.build())
				.transition(VerificationProcessTransition.advance())
				.build();
	}

	@Override
	public @NotNull VerificationProcessResult<TotpEnrollmentState> submit(
			@NotNull VerificationProcessContext<TotpEnrollmentState> context,
			@NotNull CodeVerificationInteraction interaction
	) {
		return start(context);
	}
}
