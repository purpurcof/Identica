package me.whereareiam.identica.common.verification;

import me.whereareiam.identica.common.config.defaults.VerificationDefaults;
import me.whereareiam.identica.common.config.defaults.messages.MessagesCommandDefaults;
import me.whereareiam.identica.common.verification.type.totp.TotpVerificationMethod;
import me.whereareiam.identica.common.verification.type.totp.process.TotpChallengeProcess;
import me.whereareiam.identica.common.verification.type.totp.process.TotpEnrollmentProcess;
import me.whereareiam.identica.common.verification.type.totp.step.TotpChallengeVerificationStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmCodeStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpConfirmSavedStep;
import me.whereareiam.identica.common.verification.type.totp.step.TotpSetupStep;
import me.whereareiam.identica.database.VerificationPersistenceService;
import me.whereareiam.identica.model.config.Messages;
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
		Verification verification = new VerificationDefaults().supply(new Verification());
		Messages messages = messages();
		VerificationPersistenceService persistenceService = mock(VerificationPersistenceService.class);
		TotpSetupStep setupStep = new TotpSetupStep(() -> verification, () -> messages);
		TotpConfirmCodeStep confirmCodeStep = new TotpConfirmCodeStep(() -> verification);
		TotpConfirmSavedStep confirmSavedStep = new TotpConfirmSavedStep();
		TotpChallengeVerificationStep challengeStep = new TotpChallengeVerificationStep(() -> verification, persistenceService);
		TotpVerificationMethod method = new TotpVerificationMethod(
				() -> verification,
				new TotpEnrollmentProcess(setupStep, confirmCodeStep, confirmSavedStep),
				new TotpChallengeProcess(challengeStep)
		);

		DefaultVerificationRegistry registry = new DefaultVerificationRegistry(Set.of(method));

		assertTrue(registry.find("totp").isPresent());
		assertEquals("totp", registry.find("TOTP").orElseThrow().descriptor().getId());
	}

	@Test
	void builtInTotpDescriptorAdvertisesProcessCapabilities() {
		Verification verification = new VerificationDefaults().supply(new Verification());
		Messages messages = messages();
		VerificationPersistenceService persistenceService = mock(VerificationPersistenceService.class);
		TotpSetupStep setupStep = new TotpSetupStep(() -> verification, () -> messages);
		TotpConfirmCodeStep confirmCodeStep = new TotpConfirmCodeStep(() -> verification);
		TotpConfirmSavedStep confirmSavedStep = new TotpConfirmSavedStep();
		TotpChallengeVerificationStep challengeStep = new TotpChallengeVerificationStep(() -> verification, persistenceService);
		TotpVerificationMethod method = new TotpVerificationMethod(
				() -> verification,
				new TotpEnrollmentProcess(setupStep, confirmCodeStep, confirmSavedStep),
				new TotpChallengeProcess(challengeStep)
		);

		assertTrue(method.descriptor().isBuiltIn());
		assertTrue(method.descriptor().isUserEnrollable());
		assertTrue(method.descriptor().getCapabilities().contains(VerificationMethodCapability.CHALLENGE));
		assertTrue(method.descriptor().getCapabilities().contains(VerificationMethodCapability.RECOVERY_CODES));
	}

	private Messages messages() {
		Messages messages = new Messages();
		Messages.Commands commands = new Messages.Commands();
		new MessagesCommandDefaults().supply(commands);
		messages.setCommands(commands);
		return messages;
	}
}
