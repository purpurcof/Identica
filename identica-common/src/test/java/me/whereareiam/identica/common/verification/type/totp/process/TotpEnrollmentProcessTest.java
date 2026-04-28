package me.whereareiam.identica.common.verification.type.totp.process;

import me.whereareiam.identica.common.config.template.VerificationTemplate;
import me.whereareiam.identica.common.config.template.messages.MessagesCommandsTemplate;
import me.whereareiam.identica.common.verification.type.totp.TotpCodec;
import me.whereareiam.identica.common.verification.type.totp.state.TotpEnrollmentState;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmCodeStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmSavedStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpSetupStep;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentContext;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.model.verification.interaction.SavedVerificationInteraction;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpEnrollmentProcessTest {
	@Test
	void startStoresNextCursorAfterSetupStep() {
		Verification verification = verification();
		TotpEnrollmentProcess process = process(verification);

		VerificationEnrollmentResult<TotpEnrollmentState> result = process.start(context(UUID.randomUUID(), null));

		assertEquals("totp-confirm-code", result.getState().getStepId());
		assertNotNull(result.getDisplay());
		assertFalse(result.getDisplay().getLines().isEmpty());
		assertTrue(result.getDisplay().getLines().stream().anyMatch(line -> line.contains("TOTP secret")));
		assertNotNull(result.getDisplay().getPlaceholders().get("secret"));
		assertNotNull(result.getDisplay().getPlaceholders().get("uri"));
	}

	@Test
	void invalidCodeStaysOnConfirmCodeStep() {
		Verification verification = verification();
		TotpEnrollmentProcess process = process(verification);
		UUID subjectUniqueId = UUID.randomUUID();
		VerificationEnrollmentResult<TotpEnrollmentState> started = process.start(context(subjectUniqueId, null));
		TotpEnrollmentState state = started.getState();

		VerificationEnrollmentResult<TotpEnrollmentState> result = process.submit(
				context(subjectUniqueId, state),
				state,
				CodeVerificationInteraction.builder()
						.subjectUniqueId(subjectUniqueId)
						.code("bad")
						.build()
		);

		assertEquals("totp-confirm-code", result.getState().getStepId());
		assertEquals(state.getSecret(), result.getState().getSecret());
		assertTrue(result.getState().getRecoveryCodes().isEmpty());
		assertFalse(result.getState().isVerified());
	}

	@Test
	void validCodeAdvancesToSavedConfirmation() {
		Verification verification = verification();
		TotpEnrollmentProcess process = process(verification);
		UUID subjectUniqueId = UUID.randomUUID();
		VerificationEnrollmentResult<TotpEnrollmentState> started = process.start(context(subjectUniqueId, null));
		TotpEnrollmentState state = started.getState();
		String code = TotpCodec.currentCode(
				state.getSecret(),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);

		VerificationEnrollmentResult<TotpEnrollmentState> result = process.submit(
				context(subjectUniqueId, state),
				state,
				CodeVerificationInteraction.builder()
						.subjectUniqueId(subjectUniqueId)
						.code(code)
						.build()
		);

		assertEquals("totp-confirm-saved", result.getState().getStepId());
		assertTrue(result.getState().isVerified());
		assertFalse(result.getState().getRecoveryCodes().isEmpty());
	}

	@Test
	void savedConfirmationCompletesWithoutChangingCursor() {
		Verification verification = verification();
		TotpEnrollmentProcess process = process(verification);
		UUID subjectUniqueId = UUID.randomUUID();
		VerificationEnrollmentResult<TotpEnrollmentState> started = process.start(context(subjectUniqueId, null));
		TotpEnrollmentState confirmedCodeState = confirmCode(process, verification, subjectUniqueId, started.getState());

		VerificationEnrollmentResult<TotpEnrollmentState> result = process.submit(
				context(subjectUniqueId, confirmedCodeState),
				confirmedCodeState,
				SavedVerificationInteraction.builder()
						.subjectUniqueId(subjectUniqueId)
						.build()
		);

		assertEquals("totp-confirm-saved", result.getState().getStepId());
		assertTrue(result.getState().isVerified());
		assertFalse(result.getState().getRecoveryCodes().isEmpty());
	}

	@Test
	void gotoTransitionJumpsToExplicitStep() {
		Verification verification = verification();
		TotpSetupStep setupStep = new TotpSetupStep(() -> verification, this::messages);
		TotpConfirmSavedStep confirmSavedStep = new TotpConfirmSavedStep();
		TotpEnrollmentProcess process = new TotpEnrollmentProcess(
				setupStep,
				new RedirectingConfirmCodeStep(verification, confirmSavedStep.id()),
				confirmSavedStep
		);
		UUID subjectUniqueId = UUID.randomUUID();
		VerificationEnrollmentResult<TotpEnrollmentState> started = process.start(context(subjectUniqueId, null));
		TotpEnrollmentState state = started.getState();
		String startedStepId = state.getStepId();

		VerificationEnrollmentResult<TotpEnrollmentState> result = process.submit(
				context(subjectUniqueId, state),
				state,
				CodeVerificationInteraction.builder()
						.subjectUniqueId(subjectUniqueId)
						.code("ignored")
						.build()
		);

		assertNotEquals(startedStepId, result.getState().getStepId());
		assertEquals("totp-confirm-saved", result.getState().getStepId());
	}

	private @NotNull TotpEnrollmentState confirmCode(
			@NotNull TotpEnrollmentProcess process,
			@NotNull Verification verification,
			@NotNull UUID subjectUniqueId,
			@NotNull TotpEnrollmentState state
	) {
		String code = TotpCodec.currentCode(
				state.getSecret(),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);
		return process.submit(
				context(subjectUniqueId, state),
				state,
				CodeVerificationInteraction.builder()
						.subjectUniqueId(subjectUniqueId)
						.code(code)
						.build()
		).getState();
	}

	private @NotNull TotpEnrollmentProcess process(@NotNull Verification verification) {
		return new TotpEnrollmentProcess(
				new TotpSetupStep(() -> verification, this::messages),
				new TotpConfirmCodeStep(() -> verification),
				new TotpConfirmSavedStep()
		);
	}

	private @NotNull VerificationEnrollmentContext<TotpEnrollmentState> context(
			@NotNull UUID subjectUniqueId,
			TotpEnrollmentState state
	) {
		return VerificationEnrollmentContext.<TotpEnrollmentState>builder()
				.enrollmentId("enrollment")
				.subjectUniqueId(subjectUniqueId)
				.username("player")
				.methodId("totp")
				.providerId("auth")
				.state(state)
				.build();
	}

	private @NotNull Verification verification() {
		return new VerificationTemplate().supply(new Verification());
	}

	private @NotNull Messages messages() {
		Messages messages = new Messages();
		Messages.Commands commands = new Messages.Commands();
		new MessagesCommandsTemplate().supply(commands);
		messages.setCommands(commands);
		return messages;
	}

	private static final class RedirectingConfirmCodeStep extends TotpConfirmCodeStep {
		private final @NotNull String targetStepId;

		private RedirectingConfirmCodeStep(@NotNull Verification verification, @NotNull String targetStepId) {
			super(() -> verification);
			this.targetStepId = targetStepId;
		}

		@Override
		public @NotNull VerificationProcessResult<TotpEnrollmentState> submit(
				@NotNull VerificationProcessContext<TotpEnrollmentState> context,
				@NotNull CodeVerificationInteraction interaction
		) {
			return VerificationProcessResult.waiting(
					context.getState(),
					VerificationProcessTransition.goTo(targetStepId)
			);
		}
	}
}
