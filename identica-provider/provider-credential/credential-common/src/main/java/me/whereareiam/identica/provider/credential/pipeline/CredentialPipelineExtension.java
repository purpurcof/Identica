package me.whereareiam.identica.provider.credential.pipeline;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.AllArgsConstructor;
import me.whereareiam.identica.pipeline.extension.PipelineExtension;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionBuilder;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;
import me.whereareiam.identica.provider.credential.pipeline.scenario.CredentialValidationStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.authentication.CredentialAuthenticationPasswordStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.authentication.CredentialAuthenticationVerificationStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.authentication.CredentialRecognitionStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.migration.CredentialMigrationAuthenticationStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.migration.CredentialMigrationConfirmStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.migration.CredentialMigrationRegistrationStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.registration.CredentialAccountPresenceStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.registration.CredentialRegistrationConfirmStep;
import me.whereareiam.identica.provider.credential.pipeline.scenario.registration.CredentialRegistrationStep;
import me.whereareiam.identica.type.pipeline.PipelineScope;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor(onConstructor_ = @Inject)
public class CredentialPipelineExtension implements PipelineExtension {
	private final @NotNull Provider<CredentialSettings> settingsProvider;

	// Steps
	private final @NotNull CredentialValidationStep validationStep;

	// Steps - Registration
	private final @NotNull CredentialAccountPresenceStep credentialAccountPresenceStep;
	private final @NotNull CredentialRegistrationStep credentialRegistrationStep;
	private final @NotNull CredentialRegistrationConfirmStep credentialRegistrationConfirmStep;

	// Steps - Authentication
	private final @NotNull CredentialRecognitionStep recognitionStep;
	private final @NotNull CredentialAuthenticationPasswordStep authenticationPasswordStep;
	private final @NotNull CredentialAuthenticationVerificationStep authenticationVerificationStep;

	// Steps - Migration
	private final @NotNull CredentialMigrationAuthenticationStep migrationAuthenticationStep;
	private final @NotNull CredentialMigrationRegistrationStep migrationRegistrationStep;
	private final @NotNull CredentialMigrationConfirmStep migrationConfirmStep;

	public static @NotNull String extensionId() {
		return CredentialConstants.PROVIDER_ID + ":password-auth";
	}

	@Override
	public @NotNull String id() {
		return extensionId();
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public void apply(@NotNull PipelineExtensionBuilder builder) {
		String providerId = CredentialConstants.PROVIDER_ID;
		CredentialSettings settings = settingsProvider.get();
		boolean requireRepeat = settings != null
				&& settings.getScenario() != null
				&& settings.getScenario().getRegistration() != null
				&& settings.getScenario().getRegistration().isRequireRepeat();

		registerForBothJourneyModes(
				builder,
				PipelineScope.REGISTRATION,
				providerId,
				PipelineType.REGISTRATION,
				validationStep
		);
		registerForBothJourneyModes(
				builder,
				PipelineScope.REGISTRATION,
				providerId,
				PipelineType.REGISTRATION,
				credentialAccountPresenceStep
		);
		registerForBothJourneyModes(
				builder,
				PipelineScope.REGISTRATION,
				providerId,
				PipelineType.REGISTRATION,
				credentialRegistrationStep
		);
		if (requireRepeat) {
			registerForBothJourneyModes(
					builder,
					PipelineScope.REGISTRATION,
					providerId,
					PipelineType.REGISTRATION,
					credentialRegistrationConfirmStep
			);
		}

		registerForBothJourneyModes(
				builder,
				PipelineScope.AUTHENTICATION,
				providerId,
				PipelineType.AUTHENTICATION,
				validationStep
		);
		registerForBothJourneyModes(
				builder,
				PipelineScope.AUTHENTICATION,
				providerId,
				PipelineType.AUTHENTICATION,
				recognitionStep
		);

		registerForBothJourneyModes(
				builder,
				PipelineScope.AUTHENTICATION,
				providerId,
				PipelineType.AUTHENTICATION,
				authenticationPasswordStep
		);
		registerForBothJourneyModes(
				builder,
				PipelineScope.AUTHENTICATION,
				providerId,
				PipelineType.AUTHENTICATION,
				authenticationVerificationStep
		);

		builder.registerStep(
				PipelineScope.MIGRATION,
				providerId,
				StageType.PROVIDER,
				PipelineType.MIGRATION,
				validationStep
		);
		builder.registerStep(
				PipelineScope.MIGRATION,
				providerId,
				StageType.PROVIDER,
				PipelineType.MIGRATION,
				migrationAuthenticationStep
		);
		builder.registerStep(
				PipelineScope.MIGRATION,
				providerId,
				StageType.PROVIDER,
				PipelineType.MIGRATION,
				migrationRegistrationStep
		);
		if (requireRepeat) {
			builder.registerStep(
					PipelineScope.MIGRATION,
					providerId,
					StageType.PROVIDER,
					PipelineType.MIGRATION,
					migrationConfirmStep
			);
		}
	}

	private void registerForBothJourneyModes(
			@NotNull PipelineExtensionBuilder builder,
			@NotNull PipelineScope scope,
			@NotNull String providerId,
			@NotNull PipelineType pipelineType,
			@NotNull Step step
	) {
		builder.registerStep(scope, providerId, StageType.PROVIDER, pipelineType, JourneyMode.SEAMLESS, step);
		builder.registerStep(scope, providerId, StageType.PROVIDER, pipelineType, JourneyMode.INTERACTIVE, step);
	}
}
