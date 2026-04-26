package me.whereareiam.identica.common.verification.type.totp.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.verification.RecoveryCodeGenerator;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransition;
import me.whereareiam.identica.verification.process.VerificationProcessStep;
import me.whereareiam.identica.common.verification.type.totp.TotpCodec;
import me.whereareiam.identica.common.verification.type.totp.state.TotpEnrollmentState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class TotpConfirmCodeStep implements VerificationProcessStep<CodeVerificationInteraction, TotpEnrollmentState> {
	private final Provider<Verification> verificationProvider;

	@Inject
	public TotpConfirmCodeStep(Provider<Verification> verificationProvider) {
		this.verificationProvider = verificationProvider;
	}

	@Override
	public @NotNull String id() {
		return "totp-confirm-code";
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
		return VerificationProcessResult.waiting(context.getState(), VerificationProcessTransition.stay());
	}

	@Override
	public @NotNull VerificationProcessResult<TotpEnrollmentState> submit(
			@NotNull VerificationProcessContext<TotpEnrollmentState> context,
			@NotNull CodeVerificationInteraction interaction
	) {
		TotpEnrollmentState state = context.getState();
		if (state == null)
			return VerificationProcessResult.waiting(null, VerificationProcessTransition.stay());

		if (!verify(state.getSecret(), interaction.getCode()))
			return VerificationProcessResult.<TotpEnrollmentState>builder()
					.status(me.whereareiam.identica.type.verification.VerificationProcessStatus.INVALID)
					.state(state)
					.transition(VerificationProcessTransition.stay())
					.build();

		List<String> recoveryCodes = recoveryCodes();
		state.setVerified(true);
		state.setRecoveryCodes(recoveryCodes);
		return VerificationProcessResult.waiting(state, VerificationProcessTransition.advance());
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
}
