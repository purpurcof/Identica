package me.whereareiam.identica.common.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.verification.enrollment.VerificationEnrollmentWorkflow;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.verification.challenge.VerificationChallengeEvent;
import me.whereareiam.identica.event.verification.challenge.VerificationChallengeFailedEvent;
import me.whereareiam.identica.event.verification.challenge.VerificationChallengeSucceededEvent;
import me.whereareiam.identica.event.verification.VerificationMethodDisabledEvent;
import me.whereareiam.identica.event.verification.VerificationResetEvent;
import me.whereareiam.identica.event.verification.VerificationSelectionEvent;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationActionResult;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationSelection;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;
import me.whereareiam.identica.type.verification.VerificationActionStatus;
import me.whereareiam.identica.type.verification.VerificationChallengeStatus;
import me.whereareiam.identica.verification.VerificationMethod;
import me.whereareiam.identica.verification.VerificationRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultVerificationService implements VerificationService {
	private final VerificationPersistenceService persistenceService;
	private final VerificationEnrollmentWorkflow enrollmentWorkflow;
	private final VerificationRegistry methodRegistry;
	private final Provider<Verification> verificationProvider;
	private final VerificationPolicyResolver policyResolver;
	private final EventManager eventManager;

	@Override
	public @NotNull VerificationActionResult beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull String methodId
	) {
		return enrollmentWorkflow.beginEnrollment(uniqueId, username, providerId, methodId);
	}

	@Override
	public @NotNull VerificationActionResult confirmEnrollment(@NotNull UUID uniqueId, @NotNull String value) {
		return enrollmentWorkflow.confirmEnrollment(uniqueId, value);
	}

	@Override
	public boolean cancelPendingEnrollment(@NotNull UUID uniqueId) {
		return enrollmentWorkflow.cancelPendingEnrollment(uniqueId);
	}

	@Override
	public @NotNull Optional<PendingVerificationEnrollment> findPendingEnrollment(@NotNull UUID uniqueId) {
		return enrollmentWorkflow.findPendingEnrollment(uniqueId);
	}

	@Override
	public @NotNull List<VerificationEnrollment> findEnrollments(@NotNull UUID uniqueId) {
		return persistenceService.findEnrollments(uniqueId);
	}

	@Override
	public @NotNull List<VerificationSelection> findSelections(@NotNull UUID uniqueId) {
		return persistenceService.findSelections(uniqueId);
	}

	@Override
	public @NotNull VerificationActionResult selectMethod(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@NotNull String methodId
	) {
		VerificationSelectionEvent selectionEvent = new VerificationSelectionEvent(
				uniqueId,
				providerId,
				methodId,
				false
		);
		eventManager.call(selectionEvent);
		if (selectionEvent.isCancelled()) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.NOT_ALLOWED)
					.methodId(selectionEvent.getMethodId())
					.providerId(selectionEvent.getProviderId())
					.build();
		}

		providerId = selectionEvent.getProviderId();
		methodId = selectionEvent.getMethodId();
		if (persistenceService.findEnrollment(uniqueId, methodId).isEmpty()) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.METHOD_NOT_ENROLLED)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		VerificationPolicyResolver.ResolvedMethodPolicy policy = policyResolver.resolveMethodPolicy(providerId, methodId);
		if (policy == null || !policy.enabled()) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.METHOD_UNAVAILABLE)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		persistenceService.upsertSelection(VerificationSelection.builder()
				.uniqueId(uniqueId)
				.providerId(providerId)
				.methodId(methodId)
				.selectedAt(System.currentTimeMillis())
				.build());

		return VerificationActionResult.builder()
				.status(VerificationActionStatus.UPDATED)
				.methodId(methodId)
				.providerId(providerId)
				.build();
	}

	@Override
	public @NotNull VerificationActionResult disableMethod(@NotNull UUID uniqueId, @NotNull String methodId) {
		if (persistenceService.findEnrollment(uniqueId, methodId).isEmpty()) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.METHOD_NOT_ENROLLED)
					.methodId(methodId)
					.build();
		}

		persistenceService.deleteEnrollment(uniqueId, methodId);
		persistenceService.replaceRecoveryCodes(uniqueId, methodId, List.of());

		for (VerificationSelection selection : persistenceService.findSelections(uniqueId)) {
			if (selection == null || !methodId.equalsIgnoreCase(selection.getMethodId()))
				continue;

			VerificationPolicyResolver.ResolvedMethodPolicy policy =
					policyResolver.resolveMethodPolicy(selection.getProviderId(), methodId);
			UnavailableSelectionPolicy unavailablePolicy = policy != null
					? policy.unavailableSelectionPolicy()
					: UnavailableSelectionPolicy.CLEAR_SELECTION;

			if (unavailablePolicy == UnavailableSelectionPolicy.CLEAR_SELECTION)
				persistenceService.deleteSelection(uniqueId, selection.getProviderId());
		}
		eventManager.call(new VerificationMethodDisabledEvent(uniqueId, methodId));

		return VerificationActionResult.builder()
				.status(VerificationActionStatus.DISABLED)
				.methodId(methodId)
				.build();
	}

	@Override
	public @NotNull VerificationActionResult reset(@NotNull UUID uniqueId, @Nullable String providerId) {
		boolean fullReset = providerId == null || providerId.isBlank();
		if (providerId == null || providerId.isBlank()) {
			persistenceService.deleteAll(uniqueId);
		} else {
			persistenceService.deleteProviderSelections(uniqueId, providerId);
		}
		enrollmentWorkflow.cancelPendingEnrollment(uniqueId);
		eventManager.call(new VerificationResetEvent(uniqueId, providerId, fullReset));

		return VerificationActionResult.builder()
				.status(VerificationActionStatus.RESET)
				.providerId(providerId)
				.build();
	}

	@Override
	public @NotNull VerificationChallengeResult challenge(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@Nullable String input
	) {
		VerificationPolicyResolver.ResolvedProviderPolicy providerPolicy =
				policyResolver.resolveProviderPolicy(providerId);
		if (providerPolicy == null || !providerPolicy.enabled())
			return result(VerificationChallengeStatus.SKIP, null, false);

		VerificationSelection selection = persistenceService.findSelection(uniqueId, providerId).orElse(null);
		if (selection == null || selection.getMethodId() == null || selection.getMethodId().isBlank()) {
			return providerPolicy.required()
					? result(VerificationChallengeStatus.REQUIRED_MISSING, null, false)
					: result(VerificationChallengeStatus.SKIP, null, false);
		}

		VerificationPolicyResolver.ResolvedMethodPolicy methodPolicy =
				policyResolver.resolveMethodPolicy(providerId, selection.getMethodId());
		if (methodPolicy == null || !methodPolicy.enabled())
			return handleUnavailable(uniqueId, providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		VerificationEnrollment enrollment = persistenceService.findEnrollment(uniqueId, selection.getMethodId()).orElse(null);
		if (enrollment == null || enrollment.getPayload() == null || enrollment.getPayload().isBlank())
			return handleUnavailable(uniqueId, providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		if (input == null || input.isBlank())
			return result(VerificationChallengeStatus.WAITING, selection.getMethodId(), false);

		VerificationMethod handler = methodRegistry.find(selection.getMethodId()).orElse(null);
		if (handler == null)
			return handleUnavailable(uniqueId, providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		VerificationChallengeEvent challengeEvent = new VerificationChallengeEvent(
				uniqueId,
				providerId,
				selection.getMethodId(),
				input,
				null
		);
		eventManager.call(challengeEvent);
		if (challengeEvent.getResult() != null)
			return finalizeChallenge(uniqueId, providerId, selection.getMethodId(), challengeEvent.getResult());

		if (handler.verifyChallenge(enrollment.getPayload(), input.trim(), verificationProvider.get()))
			return finalizeChallenge(
					uniqueId,
					providerId,
					selection.getMethodId(),
					result(VerificationChallengeStatus.ALLOW, selection.getMethodId(), false)
			);

		String codeHash = RecoveryCodeGenerator.hash(input);
		boolean recoveryCodeUsed = persistenceService.markRecoveryCodeUsed(
				uniqueId,
				selection.getMethodId(),
				codeHash,
				System.currentTimeMillis()
		);
		if (recoveryCodeUsed)
			return finalizeChallenge(
					uniqueId,
					providerId,
					selection.getMethodId(),
					result(VerificationChallengeStatus.ALLOW, selection.getMethodId(), true)
			);

		return finalizeChallenge(
				uniqueId,
				providerId,
				selection.getMethodId(),
				result(VerificationChallengeStatus.INVALID_INPUT, selection.getMethodId(), false)
		);
	}

	private VerificationChallengeResult handleUnavailable(
			UUID uniqueId,
			String providerId,
			String methodId,
			VerificationPolicyResolver.ResolvedProviderPolicy providerPolicy,
			VerificationPolicyResolver.ResolvedMethodPolicy methodPolicy
	) {
		UnavailableSelectionPolicy unavailablePolicy = methodPolicy != null
				? methodPolicy.unavailableSelectionPolicy()
				: providerPolicy.unavailableSelectionPolicy();
		if (unavailablePolicy == null)
			unavailablePolicy = UnavailableSelectionPolicy.CLEAR_SELECTION;

		if (unavailablePolicy == UnavailableSelectionPolicy.CLEAR_SELECTION) {
			persistenceService.deleteSelection(uniqueId, providerId);
			boolean required = methodPolicy != null ? methodPolicy.required() : providerPolicy.required();
			return required
					? result(VerificationChallengeStatus.REQUIRED_MISSING, methodId, false)
					: result(VerificationChallengeStatus.SKIP, methodId, false);
		}

		return result(VerificationChallengeStatus.METHOD_UNAVAILABLE, methodId, false);
	}

	private VerificationChallengeResult result(
			VerificationChallengeStatus status,
			String methodId,
			boolean recoveryCodeUsed
	) {
		return VerificationChallengeResult.builder()
				.status(status)
				.methodId(methodId)
				.recoveryCodeUsed(recoveryCodeUsed)
				.build();
	}

	private @NotNull VerificationChallengeResult finalizeChallenge(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@NotNull String methodId,
			@NotNull VerificationChallengeResult result
	) {
		VerificationChallengeStatus status = result.getStatus();
		if (status == VerificationChallengeStatus.ALLOW) {
			eventManager.call(new VerificationChallengeSucceededEvent(
					uniqueId,
					providerId,
					methodId,
					result.isRecoveryCodeUsed()
			));
			return result;
		}

		if (status == VerificationChallengeStatus.INVALID_INPUT) {
			eventManager.call(new VerificationChallengeFailedEvent(
					uniqueId,
					providerId,
					methodId,
					status
			));
		}

		return result;
	}
}
