package me.whereareiam.identica.common.verification.type.totp.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.common.verification.RecoveryCodeGenerator;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.model.verification.interaction.RecoveryCodeVerificationInteraction;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.type.verification.VerificationProcessStatus;
import me.whereareiam.identica.verification.VerificationInteraction;
import me.whereareiam.identica.verification.process.VerificationProcessStep;
import me.whereareiam.identica.common.verification.type.totp.TotpCodec;
import me.whereareiam.identica.common.verification.type.totp.state.TotpChallengeState;
import org.jetbrains.annotations.NotNull;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TotpChallengeVerificationStep implements VerificationProcessStep<VerificationInteraction, TotpChallengeState> {
	private final Provider<Verification> verificationProvider;
	private final VerificationPersistenceService persistenceService;

	@Override
	public @NotNull String id() {
		return "totp-verify-challenge";
	}

	@Override
	public @NotNull Class<VerificationInteraction> interactionType() {
		return VerificationInteraction.class;
	}

	@Override
	public @NotNull Class<TotpChallengeState> stateType() {
		return TotpChallengeState.class;
	}

	@Override
	public @NotNull VerificationProcessResult<TotpChallengeState> start(
			@NotNull VerificationProcessContext<TotpChallengeState> context
	) {
		TotpChallengeState state = context.getState();
		if (state == null) {
			state = TotpChallengeState.builder()
					.stepId(id())
					.attempts(0)
					.build();
		}
		return VerificationProcessResult.waiting(state);
	}

	@Override
	public @NotNull VerificationProcessResult<TotpChallengeState> submit(
			@NotNull VerificationProcessContext<TotpChallengeState> context,
			@NotNull VerificationInteraction interaction
	) {
		TotpChallengeState state = context.getState();
		if (state == null)
			state = TotpChallengeState.builder().stepId(id()).attempts(0).build();

		String code = null;
		if (interaction instanceof CodeVerificationInteraction codeInteraction)
			code = codeInteraction.getCode();
		if (interaction instanceof RecoveryCodeVerificationInteraction recoveryInteraction)
			code = recoveryInteraction.getCode();

		if (code == null || code.isBlank()) {
			Logger.debug(
					"TOTP challenge rejected blank input uniqueId=%s provider=%s purpose=%s attempts=%s",
					context.getSubjectUniqueId(),
					context.getProviderId(),
					context.getPurpose(),
					state.getAttempts()
			);
			return VerificationProcessResult.waiting(state);
		}

		String secret = context.getEnrollments().isEmpty() ? "" : context.getEnrollments().getFirst().getCredential();
		if (!secret.isBlank() && verify(secret, code)) {
			Logger.debug(
					"TOTP challenge verified uniqueId=%s provider=%s purpose=%s attempts=%s",
					context.getSubjectUniqueId(),
					context.getProviderId(),
					context.getPurpose(),
					state.getAttempts()
			);
			return VerificationProcessResult.verified(state);
		}

		String codeHash = RecoveryCodeGenerator.hash(code);
		boolean recoveryCodeUsed = persistenceService.markRecoveryCodeUsed(
				context.getSubjectUniqueId(),
				context.getMethodId(),
				codeHash,
				System.currentTimeMillis()
		);
		if (recoveryCodeUsed)
			state.setRecoveryCodeUsed(true);
		if (recoveryCodeUsed) {
			Logger.debug(
					"TOTP challenge verified with recovery code uniqueId=%s provider=%s purpose=%s attempts=%s",
					context.getSubjectUniqueId(),
					context.getProviderId(),
					context.getPurpose(),
					state.getAttempts()
			);
			return VerificationProcessResult.verified(state);
		}

		state.setAttempts(state.getAttempts() + 1);
		Logger.debug(
				"TOTP challenge invalid code uniqueId=%s provider=%s purpose=%s attempts=%s codeLength=%s",
				context.getSubjectUniqueId(),
				context.getProviderId(),
				context.getPurpose(),
				state.getAttempts(),
				code.length()
		);
		return VerificationProcessResult.<TotpChallengeState>builder()
				.status(VerificationProcessStatus.INVALID)
				.state(state)
				.build();
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
}
