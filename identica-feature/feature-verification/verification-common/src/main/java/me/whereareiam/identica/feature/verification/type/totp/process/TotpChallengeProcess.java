package me.whereareiam.identica.feature.verification.type.totp.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.feature.verification.VerificationInteraction;
import me.whereareiam.identica.feature.verification.model.challenge.VerificationChallengeContext;
import me.whereareiam.identica.feature.verification.model.challenge.VerificationChallengeResult;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessContext;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessResult;
import me.whereareiam.identica.feature.verification.process.VerificationChallengeProcess;
import me.whereareiam.identica.feature.verification.type.status.VerificationChallengeStatus;
import me.whereareiam.identica.feature.verification.type.totp.state.TotpChallengeState;
import me.whereareiam.identica.feature.verification.type.totp.step.TotpChallengeVerificationStep;
import org.jetbrains.annotations.NotNull;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TotpChallengeProcess implements VerificationChallengeProcess<TotpChallengeState> {
	private final TotpChallengeVerificationStep verificationStep;

	@Override
	public @NotNull Class<TotpChallengeState> stateType() {
		return TotpChallengeState.class;
	}

	@Override
	public @NotNull VerificationChallengeResult<TotpChallengeState> start(
			@NotNull VerificationChallengeContext<TotpChallengeState> context
	) {
		return map(context, verificationStep.start(processContext(context, null)));
	}

	@Override
	public @NotNull VerificationChallengeResult<TotpChallengeState> submit(
			@NotNull VerificationChallengeContext<TotpChallengeState> context,
			@NotNull TotpChallengeState state,
			@NotNull VerificationInteraction interaction
	) {
		return map(context, verificationStep.submit(processContext(context, state), interaction));
	}

	private VerificationProcessContext<TotpChallengeState> processContext(
			@NotNull VerificationChallengeContext<TotpChallengeState> context,
			TotpChallengeState state
	) {
		return VerificationProcessContext.<TotpChallengeState>builder()
				.subjectUniqueId(context.getSubjectUniqueId())
				.methodId(context.getMethodId())
				.providerId(context.getProviderId())
				.purpose(context.getPurpose())
				.processId(context.getChallengeId())
				.enrollments(context.getEnrollments())
				.state(state)
				.build();
	}

	private VerificationChallengeResult<TotpChallengeState> map(
			@NotNull VerificationChallengeContext<TotpChallengeState> context,
			@NotNull VerificationProcessResult<TotpChallengeState> result
	) {
		VerificationChallengeStatus status = switch (result.getStatus()) {
			case VERIFIED -> VerificationChallengeStatus.VERIFIED;
			case INVALID -> VerificationChallengeStatus.INVALID;
			default -> VerificationChallengeStatus.WAITING;
		};
		TotpChallengeState state = result.getState();
		return VerificationChallengeResult.<TotpChallengeState>builder()
				.status(status)
				.challengeId(context.getChallengeId())
				.methodId(context.getMethodId())
				.providerId(context.getProviderId())
				.required(context.isRequired())
				.recoveryCodeUsed(state != null && state.isRecoveryCodeUsed())
				.state(state)
				.display(result.getDisplay())
				.build();
	}
}
