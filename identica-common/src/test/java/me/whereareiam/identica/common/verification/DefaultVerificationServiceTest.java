package me.whereareiam.identica.common.verification;

import me.whereareiam.identica.common.config.template.VerificationTemplate;
import me.whereareiam.identica.common.verification.type.totp.TotpChallengeProcess;
import me.whereareiam.identica.common.verification.type.totp.TotpEnrollmentProcess;
import me.whereareiam.identica.common.verification.type.totp.TotpVerificationMethod;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.type.verification.VerificationMethodCapability;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DefaultVerificationServiceTest {
	@Test
	void registryFindsRegisteredMethodByDescriptorId() {
		Verification verification = new VerificationTemplate().supply(new Verification());
		VerificationPersistenceService persistenceService = mock(VerificationPersistenceService.class);
		TotpVerificationMethod method = new TotpVerificationMethod(
				() -> verification,
				new TotpEnrollmentProcess(() -> verification),
				new TotpChallengeProcess(() -> verification, persistenceService)
		);

		DefaultVerificationRegistry registry = new DefaultVerificationRegistry(Set.of(method));

		assertTrue(registry.find("totp").isPresent());
		assertEquals("totp", registry.find("TOTP").orElseThrow().descriptor().getId());
	}

	@Test
	void builtInTotpDescriptorAdvertisesProcessCapabilities() {
		Verification verification = new VerificationTemplate().supply(new Verification());
		VerificationPersistenceService persistenceService = mock(VerificationPersistenceService.class);
		TotpVerificationMethod method = new TotpVerificationMethod(
				() -> verification,
				new TotpEnrollmentProcess(() -> verification),
				new TotpChallengeProcess(() -> verification, persistenceService)
		);

		assertTrue(method.descriptor().isBuiltIn());
		assertTrue(method.descriptor().isUserEnrollable());
		assertTrue(method.descriptor().getCapabilities().contains(VerificationMethodCapability.CHALLENGE));
		assertTrue(method.descriptor().getCapabilities().contains(VerificationMethodCapability.RECOVERY_CODES));
	}
}
