package me.whereareiam.identica.common.verification.type.totp;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.verification.RecoveryCodeGenerator;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeContext;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeResult;
import me.whereareiam.identica.model.verification.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.model.verification.interaction.RecoveryCodeVerificationInteraction;
import me.whereareiam.identica.model.verification.process.VerificationProcessDisplay;
import me.whereareiam.identica.type.verification.VerificationChallengeStatus;
import me.whereareiam.identica.verification.VerificationChallengeProcess;
import me.whereareiam.identica.verification.VerificationInteraction;
import org.jetbrains.annotations.NotNull;

@Singleton
public class TotpChallengeProcess implements VerificationChallengeProcess<TotpChallengeState> {
	private final Provider<Verification> verificationProvider;
	private final VerificationPersistenceService persistenceService;

	@Inject
	public TotpChallengeProcess(
			Provider<Verification> verificationProvider,
			VerificationPersistenceService persistenceService
	) {
		this.verificationProvider = verificationProvider;
		this.persistenceService = persistenceService;
	}

	@Override
	public @NotNull Class<TotpChallengeState> stateType() {
		return TotpChallengeState.class;
	}

	@Override
	public @NotNull VerificationChallengeResult<TotpChallengeState> start(
			@NotNull VerificationChallengeContext<TotpChallengeState> context
	) {
		return result(VerificationChallengeStatus.WAITING, context, new TotpChallengeState(0), false);
	}

	@Override
	public @NotNull VerificationChallengeResult<TotpChallengeState> submit(
			@NotNull VerificationChallengeContext<TotpChallengeState> context,
			@NotNull TotpChallengeState state,
			@NotNull VerificationInteraction interaction
	) {
		String code = null;
		if (interaction instanceof CodeVerificationInteraction codeInteraction)
			code = codeInteraction.getCode();
		if (interaction instanceof RecoveryCodeVerificationInteraction recoveryInteraction)
			code = recoveryInteraction.getCode();

		if (code == null || code.isBlank())
			return result(VerificationChallengeStatus.WAITING, context, state, false);

		if (verifyTotp(context.getEnrollments().getFirst().getCredential(), code))
			return result(VerificationChallengeStatus.VERIFIED, context, state, false);

		String codeHash = RecoveryCodeGenerator.hash(code);
		boolean recoveryCodeUsed = persistenceService.markRecoveryCodeUsed(
				context.getSubjectUniqueId(),
				context.getMethodId(),
				codeHash,
				System.currentTimeMillis()
		);
		if (recoveryCodeUsed)
			return result(VerificationChallengeStatus.VERIFIED, context, state, true);

		state.setAttempts(state.getAttempts() + 1);
		return result(VerificationChallengeStatus.INVALID, context, state, false);
	}

	private boolean verifyTotp(@NotNull String secret, @NotNull String code) {
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

	private VerificationChallengeResult<TotpChallengeState> result(
			VerificationChallengeStatus status,
			VerificationChallengeContext<TotpChallengeState> context,
			TotpChallengeState state,
			boolean recoveryCodeUsed
	) {
		return VerificationChallengeResult.<TotpChallengeState>builder()
				.status(status)
				.challengeId(context.getChallengeId())
				.methodId(context.getMethodId())
				.providerId(context.getProviderId())
				.required(context.isRequired())
				.recoveryCodeUsed(recoveryCodeUsed)
				.state(state)
				.display(VerificationProcessDisplay.builder().build())
				.build();
	}
}
