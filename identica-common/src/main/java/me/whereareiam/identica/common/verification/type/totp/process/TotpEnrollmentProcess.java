package me.whereareiam.identica.common.verification.type.totp.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentContext;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransition;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransitionType;
import me.whereareiam.identica.type.verification.VerificationEnrollmentStatus;
import me.whereareiam.identica.verification.VerificationEnrollmentProcess;
import me.whereareiam.identica.verification.VerificationInteraction;
import me.whereareiam.identica.verification.VerificationProcessStep;
import me.whereareiam.identica.common.verification.type.totp.state.TotpEnrollmentState;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmCodeStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmSavedStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpSetupStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Singleton
public class TotpEnrollmentProcess implements VerificationEnrollmentProcess<TotpEnrollmentState> {
	private final TotpSetupStep setupStep;
	private final Map<String, VerificationProcessStep<?, TotpEnrollmentState>> stepById;
	private final Map<String, String> nextStepById;

	@Inject
	public TotpEnrollmentProcess(
			TotpSetupStep setupStep,
			TotpConfirmCodeStep confirmCodeStep,
			TotpConfirmSavedStep confirmSavedStep
	) {
		this.setupStep = setupStep;
		this.stepById = Map.of(
				setupStep.id(), setupStep,
				confirmCodeStep.id(), confirmCodeStep,
				confirmSavedStep.id(), confirmSavedStep
		);
		this.nextStepById = Map.of(
				setupStep.id(), confirmCodeStep.id(),
				confirmCodeStep.id(), confirmSavedStep.id()
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
				execute(context, setupStep.id(), null, null)
		);
	}

	@Override
	public @NotNull VerificationEnrollmentResult<TotpEnrollmentState> submit(
			@NotNull VerificationEnrollmentContext<TotpEnrollmentState> context,
			@NotNull TotpEnrollmentState state,
			@NotNull VerificationInteraction interaction
	) {
		VerificationProcessResult<TotpEnrollmentState> result = execute(context, state.getStepId(), state, interaction);
		VerificationEnrollmentStatus status = switch (result.getStatus()) {
			case VERIFIED -> VerificationEnrollmentStatus.ACTIVATED;
			case INVALID -> VerificationEnrollmentStatus.INVALID;
			default -> VerificationEnrollmentStatus.WAITING;
		};
		return map(status, context, result);
	}

	private @NotNull VerificationProcessResult<TotpEnrollmentState> execute(
			@NotNull VerificationEnrollmentContext<TotpEnrollmentState> context,
			@NotNull String stepId,
			@Nullable TotpEnrollmentState state,
			@Nullable VerificationInteraction interaction
	) {
		VerificationProcessStep<?, TotpEnrollmentState> rawStep = stepById.get(stepId);
		if (rawStep == null)
			throw new IllegalStateException("Unknown TOTP enrollment step: " + stepId);

		VerificationProcessResult<TotpEnrollmentState> result = executeStep(rawStep, processContext(context, state), interaction);
		return applyTransition(stepId, result);
	}

	private <I extends VerificationInteraction> @NotNull VerificationProcessResult<TotpEnrollmentState> executeStep(
			@NotNull VerificationProcessStep<?, TotpEnrollmentState> rawStep,
			@NotNull VerificationProcessContext<TotpEnrollmentState> context,
			@Nullable VerificationInteraction interaction
	) {
		@SuppressWarnings("unchecked")
		VerificationProcessStep<I, TotpEnrollmentState> step = (VerificationProcessStep<I, TotpEnrollmentState>) rawStep;
		if (interaction != null && step.interactionType().isInstance(interaction))
			return step.submit(context, step.interactionType().cast(interaction));

		return step.start(context);
	}

	private @NotNull VerificationProcessResult<TotpEnrollmentState> applyTransition(
			@NotNull String currentStepId,
			@NotNull VerificationProcessResult<TotpEnrollmentState> result
	) {
		TotpEnrollmentState state = result.getState();
		if (state == null) return result;

		VerificationProcessTransition transition = result.getTransition();
		if (transition == null) transition = VerificationProcessTransition.stay();

		String nextStepId = resolveStepId(currentStepId, transition);
		if (nextStepId != null) state.setStepId(nextStepId);

		return result.toBuilder()
				.state(state)
				.transition(transition)
				.build();
	}

	private @Nullable String resolveStepId(
			@NotNull String currentStepId,
			@NotNull VerificationProcessTransition transition
	) {
		VerificationProcessTransitionType type = transition.getType();
		if (type == VerificationProcessTransitionType.STAY)
			return currentStepId;
		if (type == VerificationProcessTransitionType.ADVANCE) {
			String nextStepId = nextStepById.get(currentStepId);
			if (nextStepId == null) throw new IllegalStateException("No next TOTP enrollment step configured for: " + currentStepId);

			return nextStepId;
		}
		if (type == VerificationProcessTransitionType.GOTO) {
			String targetStepId = transition.getTargetStepId();
			if (targetStepId == null || !stepById.containsKey(targetStepId))
				throw new IllegalStateException("Unknown TOTP enrollment transition target: " + targetStepId);

			return targetStepId;
		}
		if (type == VerificationProcessTransitionType.COMPLETE) return null;

		throw new IllegalStateException("Unsupported TOTP enrollment transition: " + type);
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
