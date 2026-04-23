package me.whereareiam.identica.common.verification;

import me.whereareiam.identica.common.config.template.ProvidersTemplate;
import me.whereareiam.identica.common.config.template.VerificationTemplate;
import me.whereareiam.identica.common.verification.enrollment.VerificationEnrollmentStore;
import me.whereareiam.identica.common.verification.enrollment.VerificationEnrollmentWorkflow;
import me.whereareiam.identica.common.verification.type.totp.TotpCodec;
import me.whereareiam.identica.common.verification.type.totp.TotpVerificationMethod;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.verification.selection.VerificationSelectionResult;
import me.whereareiam.identica.model.verification.VerificationTarget;
import me.whereareiam.identica.model.verification.VerificationAttemptResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentSession;
import me.whereareiam.identica.model.verification.VerificationRecoveryCode;
import me.whereareiam.identica.model.verification.selection.VerificationSelection;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.type.provider.ProviderState;
import me.whereareiam.identica.type.verification.status.VerificationAttemptStatus;
import me.whereareiam.identica.type.verification.status.VerificationEnrollmentStatus;
import me.whereareiam.identica.type.verification.status.VerificationSelectionStatus;
import me.whereareiam.identica.verification.VerificationRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
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

@DisplayName("Default Verification Service")
class DefaultVerificationServiceTest {
	@DisplayName("Enrollment stays pending until the user confirms that recovery codes were saved")
	@Test
	void enrollmentRequiresSavedConfirmationBeforeActivation() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		TestPendingEnrollmentState pendingState = new TestPendingEnrollmentState();
		VerificationEnrollmentStore pendingStore = pendingStore(pendingState);
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = service(persistenceService, pendingStore, verification, providers, registry, null);

		UUID uniqueId = UUID.randomUUID();
		VerificationEnrollmentResult started = service.beginEnrollment(uniqueId, "PlayerOne", "cracked", "totp");
		assertEquals(VerificationEnrollmentStatus.STARTED, started.getStatus());
		assertTrue(service.findEnrollments(uniqueId).isEmpty());

		VerificationEnrollmentSession pending = service.findPendingEnrollment(uniqueId).orElse(null);
		assertNotNull(pending);
		String code = TotpCodec.currentCode(
				pendingSecret(pending),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);

		VerificationEnrollmentResult recoveryPending = service.confirmEnrollment(uniqueId, code);
		assertEquals(VerificationEnrollmentStatus.PENDING_SAVED_CONFIRMATION, recoveryPending.getStatus());
		assertTrue(service.findEnrollments(uniqueId).isEmpty());
		assertFalse(recoveryPending.getRecoveryCodes().isEmpty());

		VerificationEnrollmentResult activated = service.confirmEnrollment(uniqueId, "saved");
		assertEquals(VerificationEnrollmentStatus.ACTIVATED, activated.getStatus());
		assertEquals(1, service.findEnrollments(uniqueId).size());
		assertEquals(verification.getTotp().getRecoveryCodes().getAmount(),
				persistenceService.findRecoveryCodes(uniqueId, "totp").size());
	}

	@DisplayName("Recovery codes can be redeemed only once")
	@Test
	void recoveryCodeCanBeUsedOnlyOnce() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		TestPendingEnrollmentState pendingState = new TestPendingEnrollmentState();
		VerificationEnrollmentStore pendingStore = pendingStore(pendingState);
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = service(persistenceService, pendingStore, verification, providers, registry, null);

		UUID uniqueId = UUID.randomUUID();
		service.beginEnrollment(uniqueId, "PlayerOne", "cracked", "totp");
		VerificationEnrollmentSession pending = service.findPendingEnrollment(uniqueId).orElseThrow();
		String code = TotpCodec.currentCode(
				pendingSecret(pending),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);
		VerificationEnrollmentResult recoveryPending = service.confirmEnrollment(uniqueId, code);
		String recoveryCode = recoveryPending.getRecoveryCodes().getFirst();
		service.confirmEnrollment(uniqueId, "saved");
		service.selectMethod(uniqueId, "cracked", "totp");

		VerificationAttemptResult first = service.attempt(uniqueId, "cracked", recoveryCode);
		assertEquals(VerificationAttemptStatus.VERIFIED, first.getStatus());
		assertTrue(first.isRecoveryCodeUsed());

		VerificationAttemptResult second = service.attempt(uniqueId, "cracked", recoveryCode);
		assertEquals(VerificationAttemptStatus.INVALID_INPUT, second.getStatus());
	}

	@DisplayName("Optional providers skip verification when no method has been selected")
	@Test
	void optionalProviderWithoutSelectionSkipsChallenge() {
		DefaultVerificationService service = service();
		UUID uniqueId = UUID.randomUUID();
		VerificationEnrollmentResult enrollment = service.beginEnrollment(uniqueId, "PlayerOne", "cracked", "totp");
		assertEquals(VerificationEnrollmentStatus.STARTED, enrollment.getStatus());

		VerificationAttemptResult result = service.attempt(uniqueId, "cracked", null);
		assertEquals(VerificationAttemptStatus.METHOD_NOT_SELECTED, result.getStatus());
		assertFalse(result.isRequired());
	}

	@DisplayName("Required providers still deny access when no method has been selected")
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
		DefaultVerificationService service = service(
				persistenceService,
				pendingStore,
				verification,
				providers,
				registry,
				null
		);

		VerificationAttemptResult result = service.attempt(UUID.randomUUID(), "cracked", null);
		assertEquals(VerificationAttemptStatus.METHOD_NOT_SELECTED, result.getStatus());
		assertTrue(result.isRequired());
	}

	@DisplayName("Clear-selection policy removes selections for unavailable methods")
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
				.setUnavailableSelectionPolicy(me.whereareiam.identica.type.verification.UnavailableSelectionPolicy.CLEAR_SELECTION);

		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = service(persistenceService, pendingStore, verification, providers, registry, null);

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

		VerificationAttemptResult result = service.attempt(uniqueId, "cracked", null);
		assertEquals(VerificationAttemptStatus.METHOD_NOT_SELECTED, result.getStatus());
		assertTrue(persistenceService.findSelection(uniqueId, "cracked").isEmpty());
	}

	@DisplayName("Keep-locked policy preserves selections for unavailable methods")
	@Test
	void keepLockedPolicyPreservesUnavailableSelection() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = service(
				persistenceService,
				pendingStore(new TestPendingEnrollmentState()),
				verification,
				providers,
				registry,
				null
		);

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

		VerificationAttemptResult result = service.attempt(uniqueId, "cracked", null);
		assertEquals(VerificationAttemptStatus.METHOD_UNAVAILABLE, result.getStatus());
		assertTrue(persistenceService.findSelection(uniqueId, "cracked").isPresent());
	}

	@DisplayName("Activation auto-selects the current provider when it is eligible")
	@Test
	void activationAutoSelectsCurrentProviderWhenEligible() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		TestPendingEnrollmentState pendingState = new TestPendingEnrollmentState();
		VerificationEnrollmentStore pendingStore = pendingStore(pendingState);
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));

		UUID uniqueId = UUID.randomUUID();
		Session session = Session.builder()
				.uniqueId(uniqueId)
				.providerId("cracked")
				.build();
		DefaultVerificationService service = service(persistenceService, pendingStore, verification, providers, registry, session);

		service.beginEnrollment(uniqueId, "PlayerOne", "cracked", "totp");
		VerificationEnrollmentSession pending = service.findPendingEnrollment(uniqueId).orElseThrow();
		String code = TotpCodec.currentCode(
				pendingSecret(pending),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);
		service.confirmEnrollment(uniqueId, code);

		VerificationEnrollmentResult activated = service.confirmEnrollment(uniqueId, "saved");
		assertEquals(VerificationEnrollmentStatus.ACTIVATED, activated.getStatus());
		assertEquals("cracked", activated.getAutoSelectedProviderId());
		assertEquals("totp", persistenceService.findSelection(uniqueId, "cracked").orElseThrow().getMethodId());
	}

	@DisplayName("Selecting a method reports unsupported when the provider lacks verification capability")
	@Test
	void selectMethodReturnsProviderUnsupportedWhenCapabilityMissing() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = service(
				persistenceService,
				pendingStore(new TestPendingEnrollmentState()),
				verification,
				providers,
				registry,
				null,
				Set.of()
		);

		UUID uniqueId = UUID.randomUUID();
		persistenceService.upsertEnrollment(VerificationEnrollment.builder()
				.uniqueId(uniqueId)
				.methodId("totp")
				.payload("secret")
				.createdAt(1L)
				.enabledAt(2L)
				.build());

		VerificationSelectionResult result = service.selectMethod(uniqueId, "cracked", "totp");
		assertEquals(VerificationSelectionStatus.PROVIDER_UNSUPPORTED, result.getStatus());
	}

	@DisplayName("Provider-selection verification targets require a selected method")
	@Test
	void resolveProviderSelectionTargetRequiresSelection() {
		DefaultVerificationService service = service();

		VerificationAttemptResult result = service.resolve(
				VerificationTarget.providerSelection(UUID.randomUUID(), "cracked", "authentication")
		);

		assertEquals(VerificationAttemptStatus.METHOD_NOT_SELECTED, result.getStatus());
	}

	@DisplayName("Method-enrollment verification targets use the enrolled method directly")
	@Test
	void verifyMethodEnrollmentTargetUsesEnrolledMethodDirectly() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		TestPendingEnrollmentState pendingState = new TestPendingEnrollmentState();
		VerificationEnrollmentStore pendingStore = pendingStore(pendingState);
		Verification verification = new VerificationTemplate().supply(new Verification());
		Providers providers = new ProvidersTemplate().supply(new Providers());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		DefaultVerificationService service = service(persistenceService, pendingStore, verification, providers, registry, null);

		UUID uniqueId = UUID.randomUUID();
		service.beginEnrollment(uniqueId, "PlayerOne", "cracked", "totp");
		VerificationEnrollmentSession pending = service.findPendingEnrollment(uniqueId).orElseThrow();
		String code = TotpCodec.currentCode(
				pendingSecret(pending),
				verification.getTotp().getDigits(),
				verification.getTotp().periodSeconds()
		);
		service.confirmEnrollment(uniqueId, code);
		service.confirmEnrollment(uniqueId, "saved");

		VerificationAttemptResult result = service.verify(
			VerificationTarget.methodEnrollment(uniqueId, "totp", "cracked", "protected-action"),
			TotpCodec.currentCode(
					pendingSecret(pending),
					verification.getTotp().getDigits(),
					verification.getTotp().periodSeconds()
			)
		);

		assertEquals(VerificationAttemptStatus.VERIFIED, result.getStatus());
	}

	private DefaultVerificationService service() {
		TestVerificationPersistenceService persistenceService = new TestVerificationPersistenceService();
		Verification verification = new VerificationTemplate().supply(new Verification());
		VerificationRegistry registry = new DefaultVerificationRegistry(Set.of(new TotpVerificationMethod()));
		return service(
				persistenceService,
				pendingStore(new TestPendingEnrollmentState()),
				verification,
				new ProvidersTemplate().supply(new Providers()),
				registry,
				null
		);
	}

	private DefaultVerificationService service(
			TestVerificationPersistenceService persistenceService,
			VerificationEnrollmentStore pendingStore,
			Verification verification,
			Providers providers,
			VerificationRegistry registry,
			@Nullable Session session
	) {
		return service(persistenceService, pendingStore, verification, providers, registry, session, Set.of(provider("cracked"), provider("premium")));
	}

	private DefaultVerificationService service(
			TestVerificationPersistenceService persistenceService,
			VerificationEnrollmentStore pendingStore,
			Verification verification,
			Providers providers,
			VerificationRegistry registry,
			@Nullable Session session,
			Set<InternalProvider> supportedProviders
	) {
		SessionService sessionService = mock(SessionService.class);
		when(sessionService.findByUniqueId(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
				CompletableFuture.completedFuture(Optional.ofNullable(session)
						.filter(value -> value.getUniqueId().equals(invocation.getArgument(0)))));
		ProviderManager providerManager = mock(ProviderManager.class);
		when(providerManager.getProviders()).thenReturn(List.copyOf(supportedProviders));

		return new DefaultVerificationService(
				persistenceService,
				new VerificationEnrollmentWorkflow(
						persistenceService,
						pendingStore,
						registry,
						() -> verification,
						mock(EventManager.class)
				),
				registry,
				() -> verification,
				new VerificationPolicyResolver(() -> providers),
				providerManager,
				sessionService,
				mock(EventManager.class)
		);
	}

	private InternalProvider provider(String id) {
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId(id);
		descriptor.setCapabilities(List.of(ProviderCapability.VERIFICATION));
		return InternalProvider.builder()
				.descriptor(descriptor)
				.priority(100)
				.state(ProviderState.ENABLED)
				.build();
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

	private String pendingSecret(@NotNull VerificationEnrollmentSession pending) {
		return pending.getMethodData().get("secret");
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
