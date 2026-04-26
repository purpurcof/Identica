package me.whereareiam.identica.common.verification.type.totp.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentContext;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.verification.process.base.VerificationProcessOrchestrator;
import me.whereareiam.identica.type.verification.VerificationEnrollmentStatus;
import me.whereareiam.identica.verification.process.VerificationEnrollmentProcess;
import me.whereareiam.identica.verification.VerificationInteraction;
import me.whereareiam.identica.verification.process.VerificationProcessStep;
import me.whereareiam.identica.common.verification.type.totp.state.TotpEnrollmentState;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmCodeStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmSavedStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpSetupStep;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Singleton
@Getter
public class TotpEnrollmentProcess implements VerificationEnrollmentProcess<TotpEnrollmentState> {
	private final VerificationProcessOrchestrator<TotpEnrollmentState> orchestrator;

	@Inject
	public TotpEnrollmentProcess(
			TotpSetupStep setupStep,
			TotpConfirmCodeStep confirmCodeStep,
			TotpConfirmSavedStep confirmSavedStep
	) {
		Map<String, VerificationProcessStep<?, TotpEnrollmentState>> stepById = Map.of(
				setupStep.id(), setupStep,
				confirmCodeStep.id(), confirmCodeStep,
				confirmSavedStep.id(), confirmSavedStep
		);
		this.orchestrator = new VerificationProcessOrchestrator<>(
				setupStep.id(),
				stepById,
				Map.of(
						setupStep.id(), confirmCodeStep.id(),
						confirmCodeStep.id(), confirmSavedStep.id()
				)
		);
	}

	@Override
	public @NotNull Class<TotpEnrollmentState> stateType() {
		return TotpEnrollmentState.class;
	}

	@Override
	public @NotNull VerificationEnrollmentResult<TotpEnrollmentState> start(
			@NotNull VerificationEnrollmentContext<TotpEnrollmentState> context
	) {
		return map(
				VerificationEnrollmentStatus.STARTED,
				context,
				orchestrator.start(processContext(context, null))
		);
	}

	@Override
	public @NotNull VerificationEnrollmentResult<TotpEnrollmentState> submit(
			@NotNull VerificationEnrollmentContext<TotpEnrollmentState> context,
			@NotNull TotpEnrollmentState state,
			@NotNull VerificationInteraction interaction
	) {
		VerificationProcessResult<TotpEnrollmentState> result = orchestrator.submit(
				state.getStepId(),
				processContext(context, state),
				interaction
		);
		VerificationEnrollmentStatus status = switch (result.getStatus()) {
			case VERIFIED -> VerificationEnrollmentStatus.ACTIVATED;
			case INVALID -> VerificationEnrollmentStatus.INVALID;
			default -> VerificationEnrollmentStatus.WAITING;
		};
		return map(status, context, result);
	}

	private VerificationProcessContext<TotpEnrollmentState> processContext(
			@NotNull VerificationEnrollmentContext<TotpEnrollmentState> context,
			TotpEnrollmentState state
	) {
		return VerificationProcessContext.<TotpEnrollmentState>builder()
				.subjectUniqueId(context.getSubjectUniqueId())
				.methodId(context.getMethodId())
				.providerId(context.getProviderId())
				.processId(context.getEnrollmentId())
				.username(context.getUsername())
				.state(state)
				.build();
	}

	private VerificationEnrollmentResult<TotpEnrollmentState> map(
			VerificationEnrollmentStatus status,
			VerificationEnrollmentContext<TotpEnrollmentState> context,
			VerificationProcessResult<TotpEnrollmentState> result
	) {
		TotpEnrollmentState state = result.getState();
		List<String> recoveryCodes = state != null ? state.getRecoveryCodes() : null;
		return VerificationEnrollmentResult.<TotpEnrollmentState>builder()
				.status(status)
				.enrollmentId(context.getEnrollmentId())
				.methodId(context.getMethodId())
				.providerId(context.getProviderId())
				.credential(state != null ? state.credential() : null)
				.label(state != null ? state.label() : null)
				.state(state)
				.display(result.getDisplay())
				.methodData(result.getDisplay() != null ? result.getDisplay().getPlaceholders() : null)
				.recoveryCodes(recoveryCodes)
				.build();
	}
}
