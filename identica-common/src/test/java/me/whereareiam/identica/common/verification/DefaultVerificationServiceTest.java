package me.whereareiam.identica.common.verification;

import me.whereareiam.identica.common.config.template.ProvidersTemplate;
import me.whereareiam.identica.common.config.template.VerificationTemplate;
import me.whereareiam.identica.common.verification.enrollment.VerificationEnrollmentStore;
import me.whereareiam.identica.common.verification.enrollment.VerificationEnrollmentWorkflow;
import me.whereareiam.identica.common.verification.type.totp.TotpCodec;
import me.whereareiam.identica.common.verification.type.totp.TotpVerificationMethod;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentSession;
import me.whereareiam.identica.model.verification.VerificationActionResult;
import me.whereareiam.identica.model.verification.challenge.VerificationChallengeResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationRecoveryCode;
import me.whereareiam.identica.model.verification.VerificationSelection;
import me.whereareiam.identica.type.verification.VerificationActionStatus;
import me.whereareiam.identica.type.verification.VerificationChallengeStatus;
import me.whereareiam.identica.verification.VerificationRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultVerificationServiceTest {
	@Test
	void enrollmentRequiresSavedConfirmationBeforeActivation() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		TestPendingEnrollmentState pendingState = new TestPendingEnrollmentState();
		VerificationEnrollmentStore pendingStore = pendingStore(pendingState);
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = new DefaultVerificationService(
				persistenceService,
				new VerificationEnrollmentWorkflow(
						persistenceService,
						pendingStore,
						registry,
						() -> verification,
						mock(EventManager.class)
				),
				() -> verification,
				new VerificationPolicyResolver(() -> providers),
				mock(EventManager.class)
		);

		UUID uniqueId = UUID.randomUUID();
		VerificationActionResult started = service.beginEnrollment(uniqueId, "PlayerOne", "cracked", "totp");
		assertEquals(VerificationActionStatus.STARTED, started.getStatus());
		assertTrue(service.findEnrollments(uniqueId).isEmpty());

		VerificationEnrollmentSession pending = service.findPendingEnrollment(uniqueId).orElse(null);
		assertNotNull(pending);
		String code = TotpCodec.currentCode(
				pending.getSecret(),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);

		VerificationActionResult recoveryPending = service.confirmEnrollment(uniqueId, code);
		assertEquals(VerificationActionStatus.PENDING_SAVED_CONFIRMATION, recoveryPending.getStatus());
		assertTrue(service.findEnrollments(uniqueId).isEmpty());
		assertFalse(recoveryPending.getRecoveryCodes().isEmpty());

		VerificationActionResult activated = service.confirmEnrollment(uniqueId, "saved");
		assertEquals(VerificationActionStatus.ACTIVATED, activated.getStatus());
		assertEquals(1, service.findEnrollments(uniqueId).size());
		assertEquals(verification.getTotp().getRecoveryCodes().getAmount(),
				persistenceService.findRecoveryCodes(uniqueId, "totp").size());
	}

	@Test
	void recoveryCodeCanBeUsedOnlyOnce() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		TestPendingEnrollmentState pendingState = new TestPendingEnrollmentState();
		VerificationEnrollmentStore pendingStore = pendingStore(pendingState);
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = new DefaultVerificationService(
				persistenceService,
				new VerificationEnrollmentWorkflow(
						persistenceService,
						pendingStore,
						registry,
						() -> verification,
						mock(EventManager.class)
				),
				() -> verification,
				new VerificationPolicyResolver(() -> providers),
				mock(EventManager.class)
		);

		UUID uniqueId = UUID.randomUUID();
		service.beginEnrollment(uniqueId, "PlayerOne", "cracked", "totp");
		VerificationEnrollmentSession pending = service.findPendingEnrollment(uniqueId).orElseThrow();
		String code = TotpCodec.currentCode(
				pending.getSecret(),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);
		VerificationActionResult recoveryPending = service.confirmEnrollment(uniqueId, code);
		String recoveryCode = recoveryPending.getRecoveryCodes().getFirst();
		service.confirmEnrollment(uniqueId, "saved");
		service.selectMethod(uniqueId, "cracked", "totp");

		VerificationChallengeResult first = service.challenge(uniqueId, "cracked", recoveryCode);
		assertEquals(VerificationChallengeStatus.ALLOW, first.getStatus());
		assertTrue(first.isRecoveryCodeUsed());

		VerificationChallengeResult second = service.challenge(uniqueId, "cracked", recoveryCode);
		assertEquals(VerificationChallengeStatus.INVALID_INPUT, second.getStatus());
	}

	@Test
	void optionalProviderWithoutSelectionSkipsChallenge() {
		DefaultVerificationService service = service();
		VerificationChallengeResult result = service.challenge(UUID.randomUUID(), "cracked", null);
		assertEquals(VerificationChallengeStatus.SKIP, result.getStatus());
	}

	@Test
	void requiredProviderWithoutSelectionDeniesChallenge() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		VerificationEnrollmentStore pendingStore = pendingStore(new TestPendingEnrollmentState());
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		providers.getProviders().stream()
				.filter(entry -> "cracked".equalsIgnoreCase(entry.getId()))
				.findFirst()
				.orElseThrow()
				.getVerification()
				.setRequired(true);

		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = new DefaultVerificationService(
				persistenceService,
				new VerificationEnrollmentWorkflow(
						persistenceService,
						pendingStore,
						registry,
						() -> verification,
						mock(EventManager.class)
				),
				() -> verification,
				new VerificationPolicyResolver(() -> providers),
				mock(EventManager.class)
		);

		VerificationChallengeResult result = service.challenge(UUID.randomUUID(), "cracked", null);
		assertEquals(VerificationChallengeStatus.REQUIRED_MISSING, result.getStatus());
	}

	@Test
	void clearSelectionPolicyRemovesUnavailableSelection() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		VerificationEnrollmentStore pendingStore = pendingStore(new TestPendingEnrollmentState());
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		providers.getProviders().stream()
				.filter(entry -> "cracked".equalsIgnoreCase(entry.getId()))
				.findFirst()
				.orElseThrow()
				.getVerification()
				.getMethods()
				.stream()
				.filter(entry -> "totp".equalsIgnoreCase(entry.getId()))
				.findFirst()
				.orElseThrow()
				.setEnabled(false);

		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = new DefaultVerificationService(
				persistenceService,
				new VerificationEnrollmentWorkflow(
						persistenceService,
						pendingStore,
						registry,
						() -> verification,
						mock(EventManager.class)
				),
				() -> verification,
				new VerificationPolicyResolver(() -> providers),
				mock(EventManager.class)
		);

		UUID uniqueId = UUID.randomUUID();
		persistenceService.upsertEnrollment(VerificationEnrollment.builder()
				.uniqueId(uniqueId)
				.methodId("totp")
				.payload("secret")
				.createdAt(1L)
				.enabledAt(2L)
				.build());
		persistenceService.upsertSelection(VerificationSelection.builder()
				.uniqueId(uniqueId)
				.providerId("cracked")
				.methodId("totp")
				.selectedAt(3L)
				.build());

		VerificationChallengeResult result = service.challenge(uniqueId, "cracked", null);
		assertEquals(VerificationChallengeStatus.SKIP, result.getStatus());
		assertTrue(persistenceService.findSelection(uniqueId, "cracked").isEmpty());
	}

	private DefaultVerificationService service() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		Verification verification = new VerificationTemplate().supply(new Verification());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		return new DefaultVerificationService(
				persistenceService,
				new VerificationEnrollmentWorkflow(
						persistenceService,
						pendingStore(new TestPendingEnrollmentState()),
						registry,
						() -> verification,
						mock(EventManager.class)
				),
				registry,
				() -> verification,
				new VerificationPolicyResolver(() -> new ProvidersTemplate().supply(new Providers())),
				mock(EventManager.class)
		);
	}

	private VerificationEnrollmentStore pendingStore(TestPendingEnrollmentState state) {
		VerificationEnrollmentStore store = mock(VerificationEnrollmentStore.class);
		doAnswer(invocation -> {
			UUID uniqueId = invocation.getArgument(0);
			VerificationEnrollmentSession pending = invocation.getArgument(1);
			state.values.put(uniqueId, pending);
			return null;
		}).when(store).put(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		when(store.peek(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
				Optional.ofNullable(state.values.get(invocation.getArgument(0))));
		when(store.consume(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
				Optional.ofNullable(state.values.remove(invocation.getArgument(0))));
		when(store.clear(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
				state.values.remove(invocation.getArgument(0)) != null);
		return store;
	}

	private static final class TestPendingEnrollmentState {
		private final Map<UUID, VerificationEnrollmentSession> values = new HashMap<>();
	}

	private static final class TestVerificationPersistenceService implements VerificationPersistenceService {
		private final Map<String, VerificationEnrollment> enrollments = new HashMap<>();
		private final Map<String, VerificationSelection> selections = new HashMap<>();
		private final Map<String, List<VerificationRecoveryCode>> recoveryCodes = new HashMap<>();

		@Override
		public @NotNull Optional<VerificationEnrollment> findEnrollment(@NotNull UUID uniqueId, @NotNull String methodId) {
			return Optional.ofNullable(enrollments.get(key(uniqueId, methodId)));
		}

		@Override
		public @NotNull List<VerificationEnrollment> findEnrollments(@NotNull UUID uniqueId) {
			return enrollments.values().stream()
					.filter(value -> uniqueId.equals(value.getUniqueId()))
					.toList();
		}

		@Override
		public @NotNull VerificationEnrollment upsertEnrollment(@NotNull VerificationEnrollment enrollment) {
			enrollments.put(key(enrollment.getUniqueId(), enrollment.getMethodId()), enrollment);
			return enrollment;
		}

		@Override
		public void deleteEnrollment(@NotNull UUID uniqueId, @NotNull String methodId) {
			enrollments.remove(key(uniqueId, methodId));
			recoveryCodes.remove(key(uniqueId, methodId));
		}

		@Override
		public @NotNull Optional<VerificationSelection> findSelection(@NotNull UUID uniqueId, @NotNull String providerId) {
			return Optional.ofNullable(selections.get(key(uniqueId, providerId)));
		}

		@Override
		public @NotNull List<VerificationSelection> findSelections(@NotNull UUID uniqueId) {
			return selections.values().stream()
					.filter(value -> uniqueId.equals(value.getUniqueId()))
					.toList();
		}

		@Override
		public @NotNull VerificationSelection upsertSelection(@NotNull VerificationSelection selection) {
			selections.put(key(selection.getUniqueId(), selection.getProviderId()), selection);
			return selection;
		}

		@Override
		public void deleteSelection(@NotNull UUID uniqueId, @NotNull String providerId) {
			selections.remove(key(uniqueId, providerId));
		}

		@Override
		public void deleteSelectionsByMethod(@NotNull UUID uniqueId, @NotNull String methodId) {
			selections.entrySet().removeIf(entry -> uniqueId.equals(entry.getValue().getUniqueId())
					&& methodId.equalsIgnoreCase(entry.getValue().getMethodId()));
		}

		@Override
		public @NotNull List<VerificationRecoveryCode> findRecoveryCodes(@NotNull UUID uniqueId, @NotNull String methodId) {
			return recoveryCodes.getOrDefault(key(uniqueId, methodId), List.of());
		}

		@Override
		public void replaceRecoveryCodes(
				@NotNull UUID uniqueId,
				@NotNull String methodId,
				@NotNull List<VerificationRecoveryCode> codes
		) {
			recoveryCodes.put(key(uniqueId, methodId), new ArrayList<>(codes));
		}

		@Override
		public boolean markRecoveryCodeUsed(@NotNull UUID uniqueId, @NotNull String methodId, @NotNull String codeHash, long usedAt) {
			List<VerificationRecoveryCode> values = recoveryCodes.get(key(uniqueId, methodId));
			if (values == null) return false;
			for (VerificationRecoveryCode value : values) {
				if (!codeHash.equals(value.getCodeHash()) || value.getUsedAt() > 0L)
					continue;
				value.setUsedAt(usedAt);
				return true;
			}
			return false;
		}

		@Override
		public void deleteAll(@NotNull UUID uniqueId) {
			enrollments.entrySet().removeIf(entry -> uniqueId.equals(entry.getValue().getUniqueId()));
			selections.entrySet().removeIf(entry -> uniqueId.equals(entry.getValue().getUniqueId()));
			recoveryCodes.entrySet().removeIf(entry -> entry.getKey().startsWith(uniqueId.toString() + ":"));
		}

		@Override
		public void deleteProviderSelections(@NotNull UUID uniqueId, @Nullable String providerId) {
			if (providerId == null || providerId.isBlank()) {
				selections.entrySet().removeIf(entry -> uniqueId.equals(entry.getValue().getUniqueId()));
				return;
			}
			selections.remove(key(uniqueId, providerId));
		}

		private String key(UUID uniqueId, String value) {
			return uniqueId + ":" + value.toLowerCase();
		}
	}
}
