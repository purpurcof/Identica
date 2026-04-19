package me.whereareiam.identica.common.verification.enrollment;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.verification.RecoveryCodeGenerator;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.verification.enroll.VerificationEnrollEvent;
import me.whereareiam.identica.event.verification.enroll.VerificationEnrollmentConfirmedEvent;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationActionResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationRecoveryCode;
import me.whereareiam.identica.type.verification.VerificationActionStatus;
import me.whereareiam.identica.type.verification.VerificationPendingStage;
import me.whereareiam.identica.verification.VerificationMethod;
import me.whereareiam.identica.verification.VerificationRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationEnrollmentWorkflow {
	private final VerificationPersistenceService persistenceService;
	private final VerificationEnrollmentStore pendingEnrollmentStore;
	private final VerificationRegistry methodRegistry;
	private final Provider<Verification> verificationProvider;
	private final EventManager eventManager;

	public @NotNull VerificationActionResult beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull String methodId
	) {
		VerificationEnrollEvent enrollEvent = new VerificationEnrollEvent(
				uniqueId,
				username,
				providerId,
				methodId,
				false
		);
		eventManager.call(enrollEvent);
		if (enrollEvent.isCancelled()) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.NOT_ALLOWED)
					.methodId(enrollEvent.getMethodId())
					.providerId(enrollEvent.getProviderId())
					.build();
		}

		providerId = enrollEvent.getProviderId();
		methodId = enrollEvent.getMethodId();
		VerificationMethod handler = methodRegistry.find(methodId).orElse(null);
		if (handler == null) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.UNKNOWN_METHOD)
					.methodId(methodId)
					.providerId(providerId)
					.build();
		}

		if (persistenceService.findEnrollment(uniqueId, handler.id()).isPresent()) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.ALREADY_ENROLLED)
					.methodId(handler.id())
					.providerId(providerId)
					.build();
		}

		PendingVerificationEnrollment pending = handler.beginEnrollment(
				uniqueId,
				username,
				providerId,
				verificationProvider.get()
		);
		pending.setStage(VerificationPendingStage.VERIFY_CODE);
		pendingEnrollmentStore.put(uniqueId, pending);
		return VerificationActionResult.builder()
				.status(VerificationActionStatus.STARTED)
				.methodId(handler.id())
				.providerId(providerId)
				.secret(pending.getSecret())
				.otpauthUri(pending.getOtpauthUri())
				.build();
	}

	public @NotNull VerificationActionResult confirmEnrollment(@NotNull UUID uniqueId, @NotNull String value) {
		PendingVerificationEnrollment pending = pendingEnrollmentStore.peek(uniqueId).orElse(null);
		if (pending == null) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.NO_PENDING)
					.build();
		}

		if (pending.getStage() == VerificationPendingStage.CONFIRM_SAVED)
			return confirmSaved(uniqueId, pending, value);

		return confirmCode(uniqueId, pending, value);
	}

	public boolean cancelPendingEnrollment(@NotNull UUID uniqueId) {
		return pendingEnrollmentStore.clear(uniqueId);
	}

	public @NotNull Optional<PendingVerificationEnrollment> findPendingEnrollment(@NotNull UUID uniqueId) {
		return pendingEnrollmentStore.peek(uniqueId);
	}

	private @NotNull VerificationActionResult confirmSaved(
			@NotNull UUID uniqueId,
			@NotNull PendingVerificationEnrollment pending,
			@NotNull String value
	) {
		if (!"saved".equalsIgnoreCase(value.trim())) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.REQUIRES_SAVED_CONFIRMATION)
					.methodId(pending.getMethodId())
					.providerId(pending.getProviderId())
					.build();
		}

		long now = System.currentTimeMillis();
		persistenceService.upsertEnrollment(VerificationEnrollment.builder()
				.uniqueId(uniqueId)
				.methodId(pending.getMethodId())
				.payload(pending.getPayload())
				.createdAt(pending.getCreatedAt())
				.enabledAt(now)
				.build());

		List<VerificationRecoveryCode> codes = new ArrayList<>();
		for (String recoveryCode : pending.getRecoveryCodes()) {
			codes.add(VerificationRecoveryCode.builder()
					.uniqueId(uniqueId)
					.methodId(pending.getMethodId())
					.codeHash(RecoveryCodeGenerator.hash(recoveryCode))
					.createdAt(now)
					.usedAt(0L)
					.build());
		}
		persistenceService.replaceRecoveryCodes(uniqueId, pending.getMethodId(), codes);
		pendingEnrollmentStore.clear(uniqueId);
		eventManager.call(new VerificationEnrollmentConfirmedEvent(
				uniqueId,
				pending.getMethodId(),
				pending.getProviderId()
		));

		return VerificationActionResult.builder()
				.status(VerificationActionStatus.ACTIVATED)
				.methodId(pending.getMethodId())
				.providerId(pending.getProviderId())
				.build();
	}

	private @NotNull VerificationActionResult confirmCode(
			@NotNull UUID uniqueId,
			@NotNull PendingVerificationEnrollment pending,
			@NotNull String value
	) {
		VerificationMethod handler = methodRegistry.find(pending.getMethodId()).orElse(null);
		if (handler == null) {
			pendingEnrollmentStore.clear(uniqueId);
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.METHOD_UNAVAILABLE)
					.methodId(pending.getMethodId())
					.providerId(pending.getProviderId())
					.build();
		}

		if (!handler.verifyEnrollment(pending, value, verificationProvider.get())) {
			return VerificationActionResult.builder()
					.status(VerificationActionStatus.INVALID_CODE)
					.methodId(pending.getMethodId())
					.providerId(pending.getProviderId())
					.build();
		}

		List<String> recoveryCodes = handler.generateRecoveryCodes(verificationProvider.get());
		pending.setRecoveryCodes(recoveryCodes);
		pending.setStage(VerificationPendingStage.CONFIRM_SAVED);
		pendingEnrollmentStore.put(uniqueId, pending);
		return VerificationActionResult.builder()
				.status(VerificationActionStatus.PENDING_SAVED_CONFIRMATION)
				.methodId(pending.getMethodId())
				.providerId(pending.getProviderId())
				.recoveryCodes(recoveryCodes)
				.build();
	}
}
