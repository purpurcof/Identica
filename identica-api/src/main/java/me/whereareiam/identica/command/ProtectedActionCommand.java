package me.whereareiam.identica.command;

import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.verification.VerificationTarget;
import me.whereareiam.identica.model.verification.VerificationAttemptResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.type.verification.status.VerificationAttemptStatus;
import me.whereareiam.identica.verification.VerificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public abstract class ProtectedActionCommand<T> extends SessionBoundCommand {
	private final @NotNull VerificationService verificationService;

	protected ProtectedActionCommand(@NotNull VerificationService verificationService) {
		this.verificationService = verificationService;
	}

	protected final boolean requiresStepUp(@NotNull UUID uniqueId) {
		return !verificationService.findEnrollments(uniqueId).isEmpty();
	}

	protected final @NotNull StepUpResult confirmStepUp(
			@NotNull UUID uniqueId,
			@NotNull String input,
			@Nullable String purpose
	) {
		Session session = sessionService().findByUniqueId(uniqueId).join().orElse(null);
		if (session == null || session.getProviderId() == null || session.getProviderId().isBlank())
			return StepUpResult.currentSessionRequired();

		VerificationAttemptResult attempt = verificationService.verify(
				VerificationTarget.providerSelection(uniqueId, session.getProviderId(), purpose),
				input
		);
		if (attempt.getStatus() == VerificationAttemptStatus.METHOD_NOT_SELECTED) {
			List<VerificationEnrollment> enrollments = verificationService.findEnrollments(uniqueId);
			if (enrollments.size() == 1) {
				attempt = verificationService.verify(
						VerificationTarget.methodEnrollment(
								uniqueId,
								enrollments.getFirst().getMethodId(),
								session.getProviderId(),
								purpose
						),
						input
				);
			} else {
				return StepUpResult.selectionRequired();
			}
		}

		if (attempt.getStatus() != VerificationAttemptStatus.VERIFIED)
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
}
