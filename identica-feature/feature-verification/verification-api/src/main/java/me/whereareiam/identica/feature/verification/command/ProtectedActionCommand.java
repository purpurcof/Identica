package me.whereareiam.identica.feature.verification.command;

import me.whereareiam.identica.command.SessionBoundCommand;
import me.whereareiam.identica.feature.verification.VerificationService;
import me.whereareiam.identica.feature.verification.model.challenge.VerificationChallengeResult;
import me.whereareiam.identica.feature.verification.model.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.feature.verification.model.resolution.VerificationResolutionRequest;
import me.whereareiam.identica.feature.verification.model.resolution.VerificationResolutionResult;
import me.whereareiam.identica.feature.verification.type.status.VerificationChallengeStatus;
import me.whereareiam.identica.feature.verification.type.status.VerificationResolutionStatus;
import me.whereareiam.identica.model.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class ProtectedActionCommand<T> extends SessionBoundCommand {
	private final @NotNull VerificationService verificationService;

	protected ProtectedActionCommand(@NotNull VerificationService verificationService) {
		this.verificationService = verificationService;
	}

	protected final boolean requiresStepUp(@NotNull UUID uniqueId) {
		return !verificationService.findEnrollments(uniqueId).isEmpty();
	}

	protected final @NotNull StepUpPreparation prepareStepUp(
			@NotNull UUID uniqueId,
			@Nullable String purpose
	) {
		Session session = sessionService().findByUniqueId(uniqueId).join().orElse(null);
		if (session == null || session.getProviderId() == null || session.getProviderId().isBlank())
			return StepUpPreparation.currentSessionRequired();

		VerificationResolutionResult resolution = verificationService.resolveVerification(
				VerificationResolutionRequest.builder()
						.uniqueId(uniqueId)
						.providerId(session.getProviderId())
						.purpose(purpose)
						.build()
		);

		if (resolution.getStatus() == VerificationResolutionStatus.WAITING
				|| resolution.getStatus() == VerificationResolutionStatus.SATISFIED)
			return StepUpPreparation.ready();

		return StepUpPreparation.selectionRequired();
	}

	protected final @NotNull StepUpResult confirmStepUp(
			@NotNull UUID uniqueId,
			@NotNull String input,
			@Nullable String purpose
	) {
		Session session = sessionService().findByUniqueId(uniqueId).join().orElse(null);
		if (session == null || session.getProviderId() == null || session.getProviderId().isBlank())
			return StepUpResult.currentSessionRequired();

		VerificationChallengeResult<?> attempt = verificationService.submitChallenge(
				uniqueId,
				session.getProviderId(),
				purpose,
				CodeVerificationInteraction.builder()
						.subjectUniqueId(uniqueId)
						.code(input)
						.build()
		);
		if (attempt.getStatus() == VerificationChallengeStatus.METHOD_NOT_SELECTED)
			return StepUpResult.selectionRequired();

		if (attempt.getStatus() != VerificationChallengeStatus.VERIFIED)
			return StepUpResult.invalidCode();

		return StepUpResult.verified();
	}

	public static final class StepUpResult {
		public enum Status {
			INVALID_CODE,
			CURRENT_SESSION_REQUIRED,
			SELECTION_REQUIRED,
			VERIFIED
		}

		private final Status status;

		private StepUpResult(@NotNull Status status) {
			this.status = status;
		}

		public static @NotNull StepUpResult invalidCode() {
			return new StepUpResult(Status.INVALID_CODE);
		}

		public static @NotNull StepUpResult currentSessionRequired() {
			return new StepUpResult(Status.CURRENT_SESSION_REQUIRED);
		}

		public static @NotNull StepUpResult selectionRequired() {
			return new StepUpResult(Status.SELECTION_REQUIRED);
		}

		public static @NotNull StepUpResult verified() {
			return new StepUpResult(Status.VERIFIED);
		}

		public @NotNull Status getStatus() {
			return status;
		}
	}

	public static final class StepUpPreparation {
		public enum Status {
			CURRENT_SESSION_REQUIRED,
			SELECTION_REQUIRED,
			READY
		}

		private final Status status;

		private StepUpPreparation(@NotNull Status status) {
			this.status = status;
		}

		public static @NotNull StepUpPreparation currentSessionRequired() {
			return new StepUpPreparation(Status.CURRENT_SESSION_REQUIRED);
		}

		public static @NotNull StepUpPreparation selectionRequired() {
			return new StepUpPreparation(Status.SELECTION_REQUIRED);
		}

		public static @NotNull StepUpPreparation ready() {
			return new StepUpPreparation(Status.READY);
		}

		public @NotNull Status getStatus() {
			return status;
		}
	}
}
