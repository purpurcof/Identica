package me.whereareiam.identica.common.verification.type.totp.step;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.verification.interaction.SavedVerificationInteraction;
import me.whereareiam.identica.model.verification.process.VerificationProcessContext;
import me.whereareiam.identica.model.verification.process.VerificationProcessResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessTransition;
import me.whereareiam.identica.verification.VerificationProcessStep;
import me.whereareiam.identica.common.verification.type.totp.state.TotpEnrollmentState;
import org.jetbrains.annotations.NotNull;

@Singleton
public class TotpConfirmSavedStep implements VerificationProcessStep<SavedVerificationInteraction, TotpEnrollmentState> {
	@Override
	public @NotNull String id() {
		return "totp-confirm-saved";
	}

	@Override
	public @NotNull Class<SavedVerificationInteraction> interactionType() {
		return SavedVerificationInteraction.class;
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
			@NotNull SavedVerificationInteraction interaction
	) {
		return VerificationProcessResult.verified(context.getState(), VerificationProcessTransition.complete());
	}
}
