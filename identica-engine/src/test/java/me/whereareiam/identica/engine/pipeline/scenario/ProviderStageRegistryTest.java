package me.whereareiam.identica.engine.pipeline.scenario;

import me.whereareiam.identica.engine.pipeline.scenario.authentication.DefaultAuthenticationStageRegistry;
import me.whereareiam.identica.engine.pipeline.scenario.migration.DefaultMigrationStageRegistry;
import me.whereareiam.identica.engine.pipeline.scenario.registration.DefaultRegistrationStageRegistry;
import me.whereareiam.identica.engine.step.EnrollmentStep;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Provider Stage Registry")
class ProviderStageRegistryTest {
	@DisplayName("Authentication provider stage continues after intermediate completion steps")
	@Test
	void authenticationProviderStageDoesNotUseCompletionResult() {
		assertFalse(providerStage(new DefaultAuthenticationStageRegistry()).isUsesCompletionResult());
	}

	@DisplayName("Registration provider stage continues after intermediate completion steps")
	@Test
	void registrationProviderStageDoesNotUseCompletionResult() {
		EnrollmentStep enrollmentStep = mock(EnrollmentStep.class);
		when(enrollmentStep.getName()).thenReturn("enrollment");

		assertFalse(providerStage(new DefaultRegistrationStageRegistry(enrollmentStep)).isUsesCompletionResult());
	}

	@DisplayName("Migration provider stage continues after intermediate completion steps")
	@Test
	void migrationProviderStageDoesNotUseCompletionResult() {
		assertFalse(providerStage(new DefaultMigrationStageRegistry()).isUsesCompletionResult());
	}

	private @NotNull JourneyStage providerStage(@NotNull me.whereareiam.identica.engine.pipeline.scenario.shared.AbstractJourneyRegistry registry) {
		JourneyStage providerStage = registry.getAllStages().stream()
				.filter(stage -> stage != null && stage.getType() == StageType.PROVIDER)
				.findFirst()
				.orElse(null);
		assertNotNull(providerStage);
		return providerStage;
	}
}
