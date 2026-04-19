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
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.VerificationDisableResult;
import me.whereareiam.identica.model.verification.VerificationResetResult;
import me.whereareiam.identica.model.verification.selection.VerificationSelectionResult;
import me.whereareiam.identica.model.verification.VerificationTarget;
import me.whereareiam.identica.model.verification.VerificationAttemptResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentSession;
import me.whereareiam.identica.model.verification.selection.VerificationSelection;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;
import me.whereareiam.identica.type.verification.status.VerificationAttemptStatus;
import me.whereareiam.identica.type.verification.status.VerificationDisableStatus;
import me.whereareiam.identica.type.verification.status.VerificationEnrollmentStatus;
import me.whereareiam.identica.type.verification.status.VerificationResetStatus;
import me.whereareiam.identica.type.verification.status.VerificationSelectionStatus;
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
	private final ProviderManager providerManager;
	private final SessionService sessionService;
	private final EventManager eventManager;

	@Override
	public @NotNull VerificationEnrollmentResult beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull String methodId
	) {
		return enrollmentWorkflow.beginEnrollment(uniqueId, username, providerId, methodId);
	}

	@Override
	public @NotNull VerificationEnrollmentResult confirmEnrollment(@NotNull UUID uniqueId, @NotNull String value) {
		VerificationEnrollmentResult result = enrollmentWorkflow.confirmEnrollment(uniqueId, value);
		if (result.getStatus() != VerificationEnrollmentStatus.ACTIVATED) return result;
		if (!verificationProvider.get().isAutoSelectCurrentProvider()) return result;

		Session session = sessionService.findByUniqueId(uniqueId).join().orElse(null);
		if (session == null || session.getProviderId() == null || session.getProviderId().isBlank()) return result;
		if (persistenceService.findSelection(uniqueId, session.getProviderId()).isPresent()) return result;

		VerificationSelectionResult selection = selectMethod(uniqueId, session.getProviderId(), result.getMethodId());
		if (selection.getStatus() == VerificationSelectionStatus.UPDATED)
			result.setAutoSelectedProviderId(session.getProviderId());

		return result;
	}

	@Override
	public boolean cancelPendingEnrollment(@NotNull UUID uniqueId) {
		return enrollmentWorkflow.cancelPendingEnrollment(uniqueId);
	}

	@Override
	public @NotNull Optional<VerificationEnrollmentSession> findPendingEnrollment(@NotNull UUID uniqueId) {
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
	public @NotNull VerificationSelectionResult selectMethod(
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
			return VerificationSelectionResult.builder()
					.status(VerificationSelectionStatus.NOT_ALLOWED)
					.methodId(selectionEvent.getMethodId())
					.providerId(selectionEvent.getProviderId())
					.build();
		}

		providerId = selectionEvent.getProviderId();
		methodId = selectionEvent.getMethodId();
		if (!policyResolver.hasConfiguredProvider(providerId)) {
			return VerificationSelectionResult.builder()
					.status(VerificationSelectionStatus.PROVIDER_NOT_FOUND)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		if (!supportsVerification(providerId)) {
			return VerificationSelectionResult.builder()
					.status(VerificationSelectionStatus.PROVIDER_UNSUPPORTED)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		VerificationPolicyResolver.ResolvedProviderPolicy providerPolicy = policyResolver.resolveProviderPolicy(providerId);
		if (providerPolicy == null || !providerPolicy.enabled()) {
			return VerificationSelectionResult.builder()
					.status(VerificationSelectionStatus.PROVIDER_VERIFICATION_DISABLED)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		VerificationPolicyResolver.ResolvedMethodPolicy policy = policyResolver.resolveMethodPolicy(providerId, methodId);
		if (policy == null || !policy.enabled()) {
			return VerificationSelectionResult.builder()
					.status(VerificationSelectionStatus.METHOD_DISABLED_FOR_PROVIDER)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		if (persistenceService.findEnrollment(uniqueId, methodId).isEmpty()) {
			return VerificationSelectionResult.builder()
					.status(VerificationSelectionStatus.METHOD_NOT_ENROLLED)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		VerificationSelection existing = persistenceService.findSelection(uniqueId, providerId).orElse(null);
		if (existing != null && methodId.equalsIgnoreCase(existing.getMethodId())) {
			return VerificationSelectionResult.builder()
					.status(VerificationSelectionStatus.ALREADY_SELECTED)
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

		return VerificationSelectionResult.builder()
				.status(VerificationSelectionStatus.UPDATED)
				.methodId(methodId)
				.providerId(providerId)
				.build();
	}

	@Override
	public @NotNull VerificationDisableResult disableMethod(@NotNull UUID uniqueId, @NotNull String methodId) {
		if (persistenceService.findEnrollment(uniqueId, methodId).isEmpty()) {
			return VerificationDisableResult.builder()
					.status(VerificationDisableStatus.METHOD_NOT_ENROLLED)
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

		return VerificationDisableResult.builder()
				.status(VerificationDisableStatus.DISABLED)
				.methodId(methodId)
				.build();
	}

	@Override
	public @NotNull VerificationResetResult reset(@NotNull UUID uniqueId, @Nullable String providerId) {
		boolean fullReset = providerId == null || providerId.isBlank();
		if (providerId == null || providerId.isBlank()) {
			persistenceService.deleteAll(uniqueId);
		} else {
			persistenceService.deleteProviderSelections(uniqueId, providerId);
		}
		enrollmentWorkflow.cancelPendingEnrollment(uniqueId);
		eventManager.call(new VerificationResetEvent(uniqueId, providerId, fullReset));

		return VerificationResetResult.builder()
				.status(VerificationResetStatus.RESET)
				.providerId(providerId)
				.build();
	}

	@Override
	public @NotNull VerificationAttemptResult resolve(@NotNull VerificationTarget target) {
		return verify(target, null);
	}

	@Override
	public @NotNull VerificationAttemptResult verify(
			@NotNull VerificationTarget target,
			@Nullable String input
	) {
		if (target.getSubjectUniqueId() == null || target.getType() == null || target.getKey() == null || target.getKey().isBlank())
			return result(VerificationAttemptStatus.METHOD_UNAVAILABLE, null, false, false);

		return switch (target.getType()) {
			case PROVIDER_SELECTION -> verifyProviderSelection(target.getSubjectUniqueId(), target.getKey(), input);
			case METHOD_ENROLLMENT -> verifyMethodEnrollment(
					target.getSubjectUniqueId(),
					target.getKey(),
					target.getProviderId(),
					input
			);
		};
	}

	private @NotNull VerificationAttemptResult verifyProviderSelection(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@Nullable String input
	) {
		VerificationPolicyResolver.ResolvedProviderPolicy providerPolicy = policyResolver.resolveProviderPolicy(providerId);
		if (!supportsVerification(providerId))
			return result(VerificationAttemptStatus.PROVIDER_UNSUPPORTED, null, false, false);
		if (providerPolicy == null || !providerPolicy.enabled())
			return result(VerificationAttemptStatus.PROVIDER_VERIFICATION_DISABLED, null, false, false);

		VerificationSelection selection = persistenceService.findSelection(uniqueId, providerId).orElse(null);
		if (selection == null || selection.getMethodId() == null || selection.getMethodId().isBlank())
			return result(VerificationAttemptStatus.METHOD_NOT_SELECTED, null, providerPolicy.required(), false);

		VerificationPolicyResolver.ResolvedMethodPolicy methodPolicy =
				policyResolver.resolveMethodPolicy(providerId, selection.getMethodId());
		if (methodPolicy == null || !methodPolicy.enabled())
			return handleUnavailable(uniqueId, providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		VerificationEnrollment enrollment = persistenceService.findEnrollment(uniqueId, selection.getMethodId()).orElse(null);
		if (enrollment == null || enrollment.getPayload() == null || enrollment.getPayload().isBlank())
			return handleUnavailable(uniqueId, providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		if (input == null || input.isBlank())
			return result(VerificationAttemptStatus.INPUT_REQUIRED, selection.getMethodId(), methodPolicy.required(), false);

		VerificationMethod handler = methodRegistry.find(selection.getMethodId()).orElse(null);
		if (handler == null)
			return handleUnavailable(uniqueId, providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		return verifyResolved(uniqueId, providerId, new ResolvedVerification(selection.getMethodId(), enrollment.getPayload(), handler), input, methodPolicy.required());
	}

	private @NotNull VerificationAttemptResult verifyMethodEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String methodId,
			@Nullable String providerId,
			@Nullable String input
	) {
		VerificationEnrollment enrollment = persistenceService.findEnrollment(uniqueId, methodId).orElse(null);
		if (enrollment == null || enrollment.getPayload() == null || enrollment.getPayload().isBlank())
			return result(VerificationAttemptStatus.METHOD_UNAVAILABLE, methodId, false, false);

		if (input == null || input.isBlank()) return result(VerificationAttemptStatus.INPUT_REQUIRED, methodId, false, false);

		VerificationMethod handler = methodRegistry.find(methodId).orElse(null);
		if (handler == null) return result(VerificationAttemptStatus.METHOD_UNAVAILABLE, methodId, false, false);

		return verifyResolved(
				uniqueId,
				providerId != null ? providerId : "",
				new ResolvedVerification(methodId, enrollment.getPayload(), handler),
				input,
				false
		);
	}

	private @NotNull VerificationAttemptResult verifyResolved(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@NotNull ResolvedVerification resolved,
			@NotNull String input,
			boolean required
	) {
		VerificationChallengeEvent challengeEvent = new VerificationChallengeEvent(
				uniqueId,
				providerId,
				resolved.methodId(),
				input,
				null
		);
		eventManager.call(challengeEvent);
		if (challengeEvent.getResult() != null)
			return finalizeAttempt(uniqueId, providerId, resolved.methodId(), challengeEvent.getResult());

		if (resolved.handler().verifyChallenge(resolved.payload(), input.trim(), verificationProvider.get()))
			return finalizeAttempt(
					uniqueId,
					providerId,
					resolved.methodId(),
					result(VerificationAttemptStatus.VERIFIED, resolved.methodId(), required, false)
			);

		String codeHash = RecoveryCodeGenerator.hash(input);
		boolean recoveryCodeUsed = persistenceService.markRecoveryCodeUsed(
				uniqueId,
				resolved.methodId(),
				codeHash,
				System.currentTimeMillis()
		);
		if (recoveryCodeUsed)
			return finalizeAttempt(
					uniqueId,
					providerId,
					resolved.methodId(),
					result(VerificationAttemptStatus.VERIFIED, resolved.methodId(), required, true)
			);

		return finalizeAttempt(
				uniqueId,
				providerId,
				resolved.methodId(),
				result(VerificationAttemptStatus.INVALID_INPUT, resolved.methodId(), required, false)
		);
	}

	private VerificationAttemptResult handleUnavailable(
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
			return result(VerificationAttemptStatus.METHOD_NOT_SELECTED, methodId, required, false);
		}

		boolean required = methodPolicy != null ? methodPolicy.required() : providerPolicy.required();
		return result(VerificationAttemptStatus.METHOD_UNAVAILABLE, methodId, required, false);
	}

	private boolean supportsVerification(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank())
			return false;
		return providerManager.getProviders().stream()
				.anyMatch(provider -> provider != null
						&& provider.getDescriptor() != null
						&& providerId.equalsIgnoreCase(provider.getDescriptor().getId())
						&& provider.getDescriptor().hasCapability(me.whereareiam.identica.type.provider.ProviderCapability.VERIFICATION));
	}

	private VerificationAttemptResult result(
			VerificationAttemptStatus status,
			String methodId,
			boolean required,
			boolean recoveryCodeUsed
	) {
		return VerificationAttemptResult.builder()
				.status(status)
				.methodId(methodId)
				.required(required)
				.recoveryCodeUsed(recoveryCodeUsed)
				.build();
	}

	private @NotNull VerificationAttemptResult finalizeAttempt(
			@NotNull UUID uniqueId,
			@NotNull String providerId,
			@NotNull String methodId,
			@NotNull VerificationAttemptResult result
	) {
		VerificationAttemptStatus status = result.getStatus();
		if (status == VerificationAttemptStatus.VERIFIED) {
			eventManager.call(new VerificationChallengeSucceededEvent(
					uniqueId,
					providerId,
					methodId,
					result.isRecoveryCodeUsed()
			));
			return result;
		}

		if (status == VerificationAttemptStatus.INVALID_INPUT) {
			eventManager.call(new VerificationChallengeFailedEvent(
					uniqueId,
					providerId,
					methodId,
					status
			));
		}

		return result;
	}

	private record ResolvedVerification(
			@NotNull String methodId,
			@NotNull String payload,
			@NotNull VerificationMethod handler
	) {
	}
}
