package me.whereareiam.identica.common.verification.enrollment;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.verification.codec.VerificationStateCodec;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.verification.enroll.VerificationEnrollEvent;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.PendingVerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentContext;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentState;
import me.whereareiam.identica.type.verification.VerificationEnrollmentStatus;
import me.whereareiam.identica.type.verification.VerificationMethodCapability;
import me.whereareiam.identica.verification.VerificationInteraction;
import me.whereareiam.identica.verification.VerificationMethod;
import me.whereareiam.identica.verification.VerificationRegistry;
import me.whereareiam.identica.verification.process.VerificationEnrollmentProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationEnrollmentLifecycle {
	private final VerificationPersistenceService persistenceService;
	private final VerificationEnrollmentStore enrollmentStore;
	private final VerificationStateCodec stateCodec;
	private final VerificationRegistry methodRegistry;
	private final Provider<Verification> verificationProvider;
	private final EventManager eventManager;
	private final VerificationEnrollmentActivator enrollmentActivator;

	public @NotNull VerificationEnrollmentResult<?> beginEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull String methodId
	) {
		VerificationEnrollEvent enrollEvent = new VerificationEnrollEvent(uniqueId, username, providerId, methodId, false);
		eventManager.call(enrollEvent);
		if (enrollEvent.isCancelled())
			return VerificationEnrollmentResult.of(
					VerificationEnrollmentStatus.NOT_ALLOWED,
					null,
					enrollEvent.getMethodId(),
					enrollEvent.getProviderId()
			);

		methodId = enrollEvent.getMethodId();
		providerId = enrollEvent.getProviderId();
		VerificationMethod method = methodRegistry.find(methodId).orElse(null);
		if (method == null)
			return VerificationEnrollmentResult.of(
					VerificationEnrollmentStatus.UNKNOWN_METHOD,
					null,
					methodId,
					providerId
			);
		if (!method.descriptor().isUserEnrollable()
				|| !method.descriptor().getCapabilities().contains(VerificationMethodCapability.USER_ENROLLABLE))
			return VerificationEnrollmentResult.of(VerificationEnrollmentStatus.NOT_ALLOWED, null, methodId, providerId);
		if (persistenceService.findEnrollment(uniqueId, method.descriptor().getId()).isPresent())
			return VerificationEnrollmentResult.of(
					VerificationEnrollmentStatus.ALREADY_ENROLLED,
					null,
					method.descriptor().getId(),
					providerId
			);

		String enrollmentId = UUID.randomUUID().toString();
		return startEnrollment(uniqueId, username, providerId, method, enrollmentId);
	}

	public @NotNull VerificationEnrollmentResult<?> submitEnrollmentInteraction(
			@NotNull UUID uniqueId,
			@NotNull VerificationInteraction interaction
	) {
		PendingVerificationEnrollment record = enrollmentStore.peek(uniqueId).orElse(null);
		if (record == null)
			return VerificationEnrollmentResult.of(VerificationEnrollmentStatus.NO_PENDING, null, null, null);

		VerificationMethod method = methodRegistry.find(record.getMethodId()).orElse(null);
		if (method == null) {
			enrollmentStore.clear(uniqueId);
			return VerificationEnrollmentResult.of(
					VerificationEnrollmentStatus.METHOD_UNAVAILABLE,
					record.getEnrollmentId(),
					record.getMethodId(),
					record.getProviderId()
			);
		}

		VerificationEnrollmentResult<?> result = submitEnrollment(record, method, interaction);
		if (result.getStatus() == VerificationEnrollmentStatus.ACTIVATED) {
			enrollmentActivator.activate(uniqueId, record, result);
			enrollmentStore.clear(uniqueId);
			return result;
		}

		if (result.getState() != null)
			storeEnrollment(record, result.getState());

		return result;
	}

	public boolean cancelPendingEnrollment(@NotNull UUID uniqueId) {
		return enrollmentStore.clear(uniqueId);
	}

	private @NotNull VerificationEnrollmentResult<?> startEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull VerificationMethod method,
			@NotNull String enrollmentId
	) {
		VerificationEnrollmentProcess<?> process = method.enrollment();
		return startEnrollmentTyped(uniqueId, username, providerId, method, enrollmentId, process);
	}

	private <S extends VerificationEnrollmentState> @NotNull VerificationEnrollmentResult<S> startEnrollmentTyped(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull VerificationMethod method,
			@NotNull String enrollmentId,
			@NotNull VerificationEnrollmentProcess<S> process
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

	private @NotNull VerificationEnrollmentResult<?> submitEnrollment(
			@NotNull PendingVerificationEnrollment record,
			@NotNull VerificationMethod method,
			@NotNull VerificationInteraction interaction
	) {
		VerificationEnrollmentProcess<?> process = method.enrollment();
		return submitEnrollmentTyped(record, process, interaction);
	}

	private <S extends VerificationEnrollmentState> @NotNull VerificationEnrollmentResult<S> submitEnrollmentTyped(
			@NotNull PendingVerificationEnrollment record,
			@NotNull VerificationEnrollmentProcess<S> process,
			@NotNull VerificationInteraction interaction
	) {
		S state = stateCodec.decode(record.getStatePayload(), process.stateType());
		if (state == null)
			return VerificationEnrollmentResult.of(
					VerificationEnrollmentStatus.METHOD_UNAVAILABLE,
					record.getEnrollmentId(),
					record.getMethodId(),
					record.getProviderId()
			);

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

	private void storeEnrollment(
			@NotNull UUID uniqueId,
			@NotNull String username,
			@Nullable String providerId,
			@NotNull String methodId,
			@NotNull String enrollmentId,
			@NotNull VerificationEnrollmentState state
	) {
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

	private void storeEnrollment(
			@NotNull PendingVerificationEnrollment record,
			@NotNull VerificationEnrollmentState state
	) {
		record.setStateType(state.getClass().getName());
		record.setStatePayload(stateCodec.encode(state));
		enrollmentStore.put(record.getUniqueId(), record);
	}
}
