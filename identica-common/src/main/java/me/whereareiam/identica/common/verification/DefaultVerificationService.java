package me.whereareiam.identica.common.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.verification.challenge.VerificationChallengeStore;
import me.whereareiam.identica.common.verification.enrollment.VerificationEnrollmentStore;
import me.whereareiam.identica.common.verification.codec.VerificationStateCodec;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.verification.VerificationMethodDisabledEvent;
import me.whereareiam.identica.event.verification.VerificationResetEvent;
import me.whereareiam.identica.event.verification.VerificationSelectionEvent;
import me.whereareiam.identica.event.verification.challenge.VerificationChallengeFailedEvent;
import me.whereareiam.identica.event.verification.challenge.VerificationChallengeStartedEvent;
import me.whereareiam.identica.event.verification.challenge.VerificationChallengeSucceededEvent;
import me.whereareiam.identica.event.verification.enroll.VerificationEnrollEvent;
import me.whereareiam.identica.event.verification.enroll.VerificationEnrollmentConfirmedEvent;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.VerificationDisableResult;
import me.whereareiam.identica.model.verification.VerificationRecoveryCode;
import me.whereareiam.identica.model.verification.VerificationResolutionRequest;
import me.whereareiam.identica.model.verification.VerificationResolutionResult;
import me.whereareiam.identica.model.verification.VerificationResetResult;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeContext;
import me.whereareiam.identica.model.verification.challenge.PendingVerificationChallenge;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeResult;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeState;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentContext;
import me.whereareiam.identica.model.verification.enrollment.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentState;
import me.whereareiam.identica.model.verification.selection.VerificationSelection;
import me.whereareiam.identica.model.verification.selection.VerificationSelectionResult;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;
import me.whereareiam.identica.type.verification.VerificationChallengeStatus;
import me.whereareiam.identica.type.verification.VerificationEnrollmentStatus;
import me.whereareiam.identica.type.verification.VerificationMethodCapability;
import me.whereareiam.identica.type.verification.VerificationResolutionStatus;
import me.whereareiam.identica.type.verification.status.VerificationDisableStatus;
import me.whereareiam.identica.type.verification.status.VerificationResetStatus;
import me.whereareiam.identica.type.verification.status.VerificationSelectionStatus;
import me.whereareiam.identica.verification.process.VerificationChallengeProcess;
import me.whereareiam.identica.verification.process.VerificationEnrollmentProcess;
import me.whereareiam.identica.verification.VerificationInteraction;
import me.whereareiam.identica.verification.VerificationMethod;
import me.whereareiam.identica.verification.VerificationRegistry;
import me.whereareiam.identica.verification.VerificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultVerificationService implements VerificationService {
	private final VerificationPersistenceService persistenceService;
	private final VerificationEnrollmentStore enrollmentStore;
	private final VerificationChallengeStore challengeStore;
	private final VerificationStateCodec stateCodec;
	private final VerificationRegistry methodRegistry;
	private final Provider<Verification> verificationProvider;
	private final VerificationPolicyResolver policyResolver;
	private final ProviderManager providerManager;
	private final SessionService sessionService;
	private final EventManager eventManager;

	@Override
	public @NotNull VerificationResolutionResult resolveVerification(@NotNull VerificationResolutionRequest request) {
		String providerId = request.getProviderId();
		VerificationPolicyResolver.ResolvedProviderPolicy providerPolicy = policyResolver.resolveProviderPolicy(providerId);
		if (!supportsVerification(providerId))
			return resolution(VerificationResolutionStatus.SKIPPED, null, null, false, false);
		if (providerPolicy == null || !providerPolicy.enabled())
			return resolution(VerificationResolutionStatus.SKIPPED, null, null, false, false);

		if (challengeStore.consumeVerified(request.getUniqueId(), providerId, request.getPurpose()))
			return resolution(VerificationResolutionStatus.SATISFIED, null, null, providerPolicy.required(), false);

		VerificationSelection selection = persistenceService.findSelection(request.getUniqueId(), providerId).orElse(null);
		if (selection == null || selection.getMethodId() == null || selection.getMethodId().isBlank()) {
			VerificationResolutionStatus status = providerPolicy.required() ? VerificationResolutionStatus.DENIED : VerificationResolutionStatus.SKIPPED;
			return resolution(status, null, null, providerPolicy.required(), false);
		}

		VerificationPolicyResolver.ResolvedMethodPolicy methodPolicy =
				policyResolver.resolveMethodPolicy(providerId, selection.getMethodId());
		if (methodPolicy == null || !methodPolicy.enabled())
			return handleResolutionUnavailable(request.getUniqueId(), providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		List<VerificationEnrollment> enrollments = persistenceService.findEnrollments(request.getUniqueId()).stream()
				.filter(enrollment -> enrollment != null && selection.getMethodId().equalsIgnoreCase(enrollment.getMethodId()))
				.toList();
		if (enrollments.isEmpty())
			return handleResolutionUnavailable(request.getUniqueId(), providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		VerificationMethod method = methodRegistry.find(selection.getMethodId()).orElse(null);
		if (method == null)
			return handleResolutionUnavailable(request.getUniqueId(), providerId, selection.getMethodId(), providerPolicy, methodPolicy);

		PendingVerificationChallenge existing = challengeStore.findActive(
				request.getUniqueId(),
				providerId,
				request.getPurpose()
		).orElse(null);
		if (existing != null) return resolution(VerificationResolutionStatus.WAITING, existing.getChallengeId(), existing.getMethodId(), methodPolicy.required(), false);

		VerificationChallengeResult<?> challenge = startChallenge(
				request.getUniqueId(),
				providerId,
				request.getPurpose(),
				enrollments,
				method,
				methodPolicy.required()
		);

		return resolution(
				toResolutionStatus(challenge.getStatus()),
				challenge.getChallengeId(),
				challenge.getMethodId(),
				challenge.isRequired(),
				challenge.isRecoveryCodeUsed()
		);
	}

	@Override
	public @NotNull VerificationChallengeResult<?> submitChallengeInteraction(
			@NotNull String challengeId,
			@NotNull VerificationInteraction interaction
	) {
		PendingVerificationChallenge record = challengeStore.find(challengeId).orElse(null);
		if (record == null)
			return challenge(VerificationChallengeStatus.EXPIRED, challengeId, null, null, false);

		return submit(record, interaction);
	}

	@Override
	public @NotNull VerificationChallengeResult<?> submitChallengeInteraction(
			@NotNull UUID uniqueId,
			@Nullable String providerId,
			@Nullable String purpose,
			@NotNull VerificationInteraction interaction
	) {
		PendingVerificationChallenge record = challengeStore.findActive(uniqueId, providerId, purpose).orElse(null);
		if (record == null)
			return challenge(VerificationChallengeStatus.METHOD_NOT_SELECTED, null, null, providerId, false);

		return submit(record, interaction);
	}

	@Override
	public @NotNull VerificationEnrollmentResult<?> beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull String methodId
	) {
		VerificationEnrollEvent enrollEvent = new VerificationEnrollEvent(uniqueId, username, providerId, methodId, false);
		eventManager.call(enrollEvent);
		if (enrollEvent.isCancelled())
			return enrollment(VerificationEnrollmentStatus.NOT_ALLOWED, null, enrollEvent.getMethodId(), enrollEvent.getProviderId());

		methodId = enrollEvent.getMethodId();
		providerId = enrollEvent.getProviderId();
		VerificationMethod method = methodRegistry.find(methodId).orElse(null);
		if (method == null)
			return enrollment(VerificationEnrollmentStatus.UNKNOWN_METHOD, null, methodId, providerId);
		if (!method.descriptor().isUserEnrollable()
				|| !method.descriptor().getCapabilities().contains(VerificationMethodCapability.USER_ENROLLABLE))
			return enrollment(VerificationEnrollmentStatus.NOT_ALLOWED, null, methodId, providerId);
		if (persistenceService.findEnrollment(uniqueId, method.descriptor().getId()).isPresent())
			return enrollment(VerificationEnrollmentStatus.ALREADY_ENROLLED, null, method.descriptor().getId(), providerId);

		String enrollmentId = UUID.randomUUID().toString();
		return startEnrollment(uniqueId, username, providerId, method, enrollmentId);
	}

	@Override
	public @NotNull VerificationEnrollmentResult<?> submitEnrollmentInteraction(
			@NotNull UUID uniqueId,
			@NotNull VerificationInteraction interaction
	) {
		PendingVerificationEnrollment record = enrollmentStore.peek(uniqueId).orElse(null);
		if (record == null)
			return enrollment(VerificationEnrollmentStatus.NO_PENDING, null, null, null);

		VerificationMethod method = methodRegistry.find(record.getMethodId()).orElse(null);
		if (method == null) {
			enrollmentStore.clear(uniqueId);
			return enrollment(VerificationEnrollmentStatus.METHOD_UNAVAILABLE, record.getEnrollmentId(), record.getMethodId(), record.getProviderId());
		}

		VerificationEnrollmentResult<?> result = submitEnrollment(record, method, interaction);
		if (result.getStatus() == VerificationEnrollmentStatus.ACTIVATED) {
			activateEnrollment(uniqueId, record, result);
			enrollmentStore.clear(uniqueId);
			return result;
		}

		if (result.getState() != null)
			storeEnrollment(record, result.getState());

		return result;
	}

	@Override
	public boolean cancelPendingEnrollment(@NotNull UUID uniqueId) {
		return enrollmentStore.clear(uniqueId);
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
		VerificationSelectionEvent selectionEvent = new VerificationSelectionEvent(uniqueId, providerId, methodId, false);
		eventManager.call(selectionEvent);
		if (selectionEvent.isCancelled())
			return selection(VerificationSelectionStatus.NOT_ALLOWED, selectionEvent.getMethodId(), selectionEvent.getProviderId());

		providerId = selectionEvent.getProviderId();
		methodId = selectionEvent.getMethodId();
		if (!policyResolver.hasConfiguredProvider(providerId))
			return selection(VerificationSelectionStatus.PROVIDER_NOT_FOUND, methodId, providerId);
		if (!supportsVerification(providerId))
			return selection(VerificationSelectionStatus.PROVIDER_UNSUPPORTED, methodId, providerId);

		VerificationPolicyResolver.ResolvedProviderPolicy providerPolicy = policyResolver.resolveProviderPolicy(providerId);
		if (providerPolicy == null || !providerPolicy.enabled())
			return selection(VerificationSelectionStatus.PROVIDER_VERIFICATION_DISABLED, methodId, providerId);

		VerificationPolicyResolver.ResolvedMethodPolicy policy = policyResolver.resolveMethodPolicy(providerId, methodId);
		if (policy == null || !policy.enabled())
			return selection(VerificationSelectionStatus.METHOD_DISABLED_FOR_PROVIDER, methodId, providerId);
		if (methodRegistry.find(methodId).isEmpty())
			return selection(VerificationSelectionStatus.METHOD_DISABLED_FOR_PROVIDER, methodId, providerId);
		if (persistenceService.findEnrollment(uniqueId, methodId).isEmpty())
			return selection(VerificationSelectionStatus.METHOD_NOT_ENROLLED, methodId, providerId);

		VerificationSelection existing = persistenceService.findSelection(uniqueId, providerId).orElse(null);
		if (existing != null && methodId.equalsIgnoreCase(existing.getMethodId()))
			return selection(VerificationSelectionStatus.ALREADY_SELECTED, methodId, providerId);

		persistenceService.upsertSelection(VerificationSelection.builder()
				.uniqueId(uniqueId)
				.providerId(providerId)
				.methodId(methodId)
				.selectedAt(System.currentTimeMillis())
				.build());
		return selection(VerificationSelectionStatus.UPDATED, methodId, providerId);
	}

	@Override
	public @NotNull VerificationDisableResult disableMethod(@NotNull UUID uniqueId, @NotNull String methodId) {
		if (persistenceService.findEnrollment(uniqueId, methodId).isEmpty())
			return VerificationDisableResult.builder()
					.status(VerificationDisableStatus.METHOD_NOT_ENROLLED)
					.methodId(methodId)
					.build();

		persistenceService.deleteEnrollment(uniqueId, methodId);
		persistenceService.replaceRecoveryCodes(uniqueId, methodId, List.of());
		for (VerificationSelection selection : persistenceService.findSelections(uniqueId)) {
			if (selection == null || !methodId.equalsIgnoreCase(selection.getMethodId())) continue;

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
		if (fullReset) {
			persistenceService.deleteAll(uniqueId);
		} else {
			persistenceService.deleteProviderSelections(uniqueId, providerId);
		}
		enrollmentStore.clear(uniqueId);
		eventManager.call(new VerificationResetEvent(uniqueId, providerId, fullReset));
		return VerificationResetResult.builder()
				.status(VerificationResetStatus.RESET)
				.providerId(providerId)
				.build();
	}

	private VerificationEnrollmentResult<?> startEnrollment(
			UUID uniqueId,
			String username,
			String providerId,
			VerificationMethod method,
			String enrollmentId
	) {
		VerificationEnrollmentProcess<?> process = method.enrollment();
		return startEnrollmentTyped(uniqueId, username, providerId, method, enrollmentId, process);
	}

	private <S extends VerificationEnrollmentState> VerificationEnrollmentResult<S> startEnrollmentTyped(
			UUID uniqueId,
			String username,
			String providerId,
			VerificationMethod method,
			String enrollmentId,
			VerificationEnrollmentProcess<S> process
	) {
		VerificationEnrollmentContext<S> context = VerificationEnrollmentContext.<S>builder()
				.enrollmentId(enrollmentId)
				.subjectUniqueId(uniqueId)
				.username(username)
				.methodId(method.descriptor().getId())
				.providerId(providerId)
				.build();
		VerificationEnrollmentResult<S> result = process.start(context);
		if (result.getState() != null)
			storeEnrollment(uniqueId, username, providerId, method.descriptor().getId(), enrollmentId, result.getState());
		return result;
	}

	private VerificationEnrollmentResult<?> submitEnrollment(
			PendingVerificationEnrollment record,
			VerificationMethod method,
			VerificationInteraction interaction
	) {
		VerificationEnrollmentProcess<?> process = method.enrollment();
		return submitEnrollmentTyped(record, process, interaction);
	}

	private <S extends VerificationEnrollmentState> VerificationEnrollmentResult<S> submitEnrollmentTyped(
			PendingVerificationEnrollment record,
			VerificationEnrollmentProcess<S> process,
			VerificationInteraction interaction
	) {
		S state = stateCodec.decode(record.getStatePayload(), process.stateType());
		if (state == null)
			return enrollment(VerificationEnrollmentStatus.METHOD_UNAVAILABLE, record.getEnrollmentId(), record.getMethodId(), record.getProviderId());

		VerificationEnrollmentContext<S> context = VerificationEnrollmentContext.<S>builder()
				.enrollmentId(record.getEnrollmentId())
				.subjectUniqueId(record.getUniqueId())
				.username(record.getUsername())
				.methodId(record.getMethodId())
				.providerId(record.getProviderId())
				.state(state)
				.build();
		return process.submit(context, state, interaction);
	}

	private void activateEnrollment(UUID uniqueId, PendingVerificationEnrollment record, VerificationEnrollmentResult<?> result) {
		VerificationEnrollmentState state = result.getState();
		long now = System.currentTimeMillis();
		persistenceService.upsertEnrollment(VerificationEnrollment.builder()
				.uniqueId(uniqueId)
				.methodId(record.getMethodId())
				.enrollmentId(result.getEnrollmentId() != null && !result.getEnrollmentId().isBlank()
						? result.getEnrollmentId()
						: "default")
				.credential(state != null ? state.credential() : "")
				.label(state != null ? state.label() : "")
				.createdAt(record.getCreatedAt())
				.enabledAt(now)
				.build());

		List<VerificationRecoveryCode> codes = new ArrayList<>();
		if (result.getRecoveryCodes() != null) {
			for (String recoveryCode : result.getRecoveryCodes()) {
				codes.add(VerificationRecoveryCode.builder()
						.uniqueId(uniqueId)
						.methodId(record.getMethodId())
						.codeHash(RecoveryCodeGenerator.hash(recoveryCode))
						.createdAt(now)
						.usedAt(0L)
						.build());
			}
		}
		persistenceService.replaceRecoveryCodes(uniqueId, record.getMethodId(), codes);
		eventManager.call(new VerificationEnrollmentConfirmedEvent(uniqueId, record.getMethodId(), record.getProviderId()));
		autoSelectCurrentProvider(uniqueId, record.getProviderId(), record.getMethodId(), result);
	}

	private void autoSelectCurrentProvider(
			UUID uniqueId,
			String providerId,
			String methodId,
			VerificationEnrollmentResult<?> result
	) {
		if (!verificationProvider.get().isAutoSelectCurrentProvider()) return;

		String resolvedProviderId = providerId;
		if (resolvedProviderId == null || resolvedProviderId.isBlank()) {
			Session session = sessionService.findByUniqueId(uniqueId).join().orElse(null);
			resolvedProviderId = session != null ? session.getProviderId() : null;
		}
		if (resolvedProviderId == null || resolvedProviderId.isBlank()) return;
		if (persistenceService.findSelection(uniqueId, resolvedProviderId).isPresent()) return;

		VerificationSelectionResult selection = selectMethod(uniqueId, resolvedProviderId, methodId);
		if (selection.getStatus() == VerificationSelectionStatus.UPDATED)
			result.setAutoSelectedProviderId(resolvedProviderId);
	}

	private VerificationChallengeResult<?> startChallenge(
			UUID uniqueId,
			String providerId,
			String purpose,
			List<VerificationEnrollment> enrollments,
			VerificationMethod method,
			boolean required
	) {
		VerificationChallengeProcess<?> process = method.challenge();
		return startChallengeTyped(uniqueId, providerId, purpose, enrollments, method, required, process);
	}

	private <S extends VerificationChallengeState> VerificationChallengeResult<S> startChallengeTyped(
			UUID uniqueId,
			String providerId,
			String purpose,
			List<VerificationEnrollment> enrollments,
			VerificationMethod method,
			boolean required,
			VerificationChallengeProcess<S> process
	) {
		String challengeId = UUID.randomUUID().toString();
		VerificationChallengeContext<S> context = VerificationChallengeContext.<S>builder()
				.challengeId(challengeId)
				.subjectUniqueId(uniqueId)
				.methodId(method.descriptor().getId())
				.enrollments(enrollments)
				.providerId(providerId)
				.purpose(purpose)
				.required(required)
				.build();
		VerificationChallengeResult<S> result = process.start(context);
		S state = result.getState();
		if (state != null) {
			challengeStore.put(PendingVerificationChallenge.builder()
					.challengeId(challengeId)
					.uniqueId(uniqueId)
					.methodId(method.descriptor().getId())
					.providerId(providerId)
					.purpose(purpose)
					.required(required)
					.stateType(state.getClass().getName())
					.statePayload(stateCodec.encode(state))
					.createdAt(System.currentTimeMillis())
					.expiresAt(System.currentTimeMillis() + verificationProvider.get().challengeTtlMillis())
					.build());
		}
		result.setChallengeId(challengeId);
		result.setMethodId(method.descriptor().getId());
		result.setProviderId(providerId);
		result.setRequired(required);
		eventManager.call(new VerificationChallengeStartedEvent(uniqueId, providerId, method.descriptor().getId(), challengeId));
		return result;
	}

	private VerificationChallengeResult<?> submit(PendingVerificationChallenge record, VerificationInteraction interaction) {
		VerificationMethod method = methodRegistry.find(record.getMethodId()).orElse(null);
		if (method == null)
			return challenge(VerificationChallengeStatus.METHOD_UNAVAILABLE, record.getChallengeId(), record.getMethodId(), record.getProviderId(), record.isRequired());

		VerificationChallengeResult<?> result = submitTyped(record, method.challenge(), interaction);
		if (result.getStatus() == VerificationChallengeStatus.VERIFIED) {
			challengeStore.markVerified(record);
			eventManager.call(new VerificationChallengeSucceededEvent(
					record.getUniqueId(),
					record.getProviderId(),
					record.getMethodId(),
					result.isRecoveryCodeUsed()
			));
			return result;
		}

		if (result.getStatus() == VerificationChallengeStatus.INVALID)
			eventManager.call(new VerificationChallengeFailedEvent(
					record.getUniqueId(),
					record.getProviderId(),
					record.getMethodId(),
					result.getStatus()
			));

		if (result.getState() != null) {
			VerificationChallengeState state = result.getState();
			record.setStateType(state.getClass().getName());
			record.setStatePayload(stateCodec.encode(state));
			challengeStore.put(record);
		}
		return result;
	}

	private <S extends VerificationChallengeState> VerificationChallengeResult<S> submitTyped(
			PendingVerificationChallenge record,
			VerificationChallengeProcess<S> process,
			VerificationInteraction interaction
	) {
		S state = stateCodec.decode(record.getStatePayload(), process.stateType());
		List<VerificationEnrollment> enrollments = persistenceService.findEnrollments(record.getUniqueId()).stream()
				.filter(enrollment -> enrollment != null && record.getMethodId().equalsIgnoreCase(enrollment.getMethodId()))
				.toList();
		if (state == null || enrollments.isEmpty())
			return challenge(VerificationChallengeStatus.METHOD_UNAVAILABLE, record.getChallengeId(), record.getMethodId(), record.getProviderId(), record.isRequired());

		VerificationChallengeContext<S> context = VerificationChallengeContext.<S>builder()
				.challengeId(record.getChallengeId())
				.subjectUniqueId(record.getUniqueId())
				.methodId(record.getMethodId())
				.enrollments(enrollments)
				.providerId(record.getProviderId())
				.purpose(record.getPurpose())
				.required(record.isRequired())
				.state(state)
				.build();
		return process.submit(context, state, interaction);
	}

	private void storeEnrollment(UUID uniqueId, String username, String providerId, String methodId, String enrollmentId, VerificationEnrollmentState state) {
		enrollmentStore.put(uniqueId, PendingVerificationEnrollment.builder()
				.enrollmentId(enrollmentId)
				.uniqueId(uniqueId)
				.username(username)
				.methodId(methodId)
				.providerId(providerId)
				.stateType(state.getClass().getName())
				.statePayload(stateCodec.encode(state))
				.createdAt(System.currentTimeMillis())
				.expiresAt(System.currentTimeMillis() + verificationProvider.get().enrollmentTtlMillis())
				.build());
	}

	private void storeEnrollment(PendingVerificationEnrollment record, VerificationEnrollmentState state) {
		record.setStateType(state.getClass().getName());
		record.setStatePayload(stateCodec.encode(state));
		enrollmentStore.put(record.getUniqueId(), record);
	}

	private VerificationResolutionResult handleResolutionUnavailable(
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
		if (unavailablePolicy == UnavailableSelectionPolicy.CLEAR_SELECTION)
			persistenceService.deleteSelection(uniqueId, providerId);

		boolean required = methodPolicy != null ? methodPolicy.required() : providerPolicy.required();
		return resolution(required ? VerificationResolutionStatus.DENIED : VerificationResolutionStatus.SKIPPED, null, methodId, required, false);
	}

	private boolean supportsVerification(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return false;
		return providerManager.getProviders().stream()
				.anyMatch(provider -> provider != null
						&& provider.getDescriptor() != null
						&& providerId.equalsIgnoreCase(provider.getDescriptor().getId())
						&& provider.getDescriptor().hasCapability(ProviderCapability.VERIFICATION));
	}

	private VerificationResolutionStatus toResolutionStatus(VerificationChallengeStatus status) {
		if (status == VerificationChallengeStatus.VERIFIED) return VerificationResolutionStatus.SATISFIED;
		if (status == VerificationChallengeStatus.WAITING || status == VerificationChallengeStatus.INVALID)
			return VerificationResolutionStatus.WAITING;
		if (status == VerificationChallengeStatus.PROVIDER_UNSUPPORTED
				|| status == VerificationChallengeStatus.PROVIDER_VERIFICATION_DISABLED)
			return VerificationResolutionStatus.SKIPPED;
		return VerificationResolutionStatus.DENIED;
	}

	private VerificationResolutionResult resolution(
			VerificationResolutionStatus status,
			String challengeId,
			String methodId,
			boolean required,
			boolean recoveryCodeUsed
	) {
		return VerificationResolutionResult.builder()
				.status(status)
				.challengeId(challengeId)
				.methodId(methodId)
				.required(required)
				.recoveryCodeUsed(recoveryCodeUsed)
				.build();
	}

	private <S extends VerificationChallengeState> VerificationChallengeResult<S> challenge(
			VerificationChallengeStatus status,
			String challengeId,
			String methodId,
			String providerId,
			boolean required
	) {
		return VerificationChallengeResult.<S>builder()
				.status(status)
				.challengeId(challengeId)
				.methodId(methodId)
				.providerId(providerId)
				.required(required)
				.recoveryCodeUsed(false)
				.state(null)
				.build();
	}

	private <S extends VerificationEnrollmentState> VerificationEnrollmentResult<S> enrollment(
			VerificationEnrollmentStatus status,
			String enrollmentId,
			String methodId,
			String providerId
	) {
		return VerificationEnrollmentResult.<S>builder()
				.status(status)
				.enrollmentId(enrollmentId)
				.methodId(methodId)
				.providerId(providerId)
				.build();
	}

	private VerificationSelectionResult selection(VerificationSelectionStatus status, String methodId, String providerId) {
		return VerificationSelectionResult.builder()
				.status(status)
				.methodId(methodId)
				.providerId(providerId)
				.build();
	}
}
