package me.whereareiam.identica.provider.cracked.pipeline;

import com.google.inject.Provider;
import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import me.whereareiam.identica.pipeline.extension.PipelineExtension;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionBuilder;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.CrackedValidationStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.authentication.CrackedSessionReuseStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.authentication.CrackedAuthenticationPasswordStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.migration.CrackedMigrationAuthenticationStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.migration.CrackedMigrationConfirmStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.migration.CrackedMigrationRegistrationStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.registration.CrackedRegistrationConfirmStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.registration.CrackedAccountPresenceStep;
import me.whereareiam.identica.provider.cracked.pipeline.scenario.registration.CrackedRegistrationStep;
import me.whereareiam.identica.type.pipeline.PipelineScope;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor(onConstructor_ = @Inject)
public class CrackedPipelineExtension implements PipelineExtension {
	private final @NotNull Provider<CrackedSettings> settingsProvider;

	// Steps
	private final @NotNull CrackedValidationStep validationStep;

	// Steps - Registration
	private final @NotNull CrackedAccountPresenceStep crackedAccountPresenceStep;
	private final @NotNull CrackedRegistrationStep crackedRegistrationStep;
	private final @NotNull CrackedRegistrationConfirmStep crackedRegistrationConfirmStep;

	// Steps - Authentication
	private final @NotNull CrackedSessionReuseStep sessionReuseStep;
	private final @NotNull CrackedAuthenticationPasswordStep authenticationPasswordStep;

	// Steps - Migration
	private final @NotNull CrackedMigrationAuthenticationStep migrationAuthenticationStep;
	private final @NotNull CrackedMigrationRegistrationStep migrationRegistrationStep;
	private final @NotNull CrackedMigrationConfirmStep migrationConfirmStep;

	public static @NotNull String extensionId() {
		return CrackedConstants.PROVIDER_ID + ":cracked-auth";
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
		String providerId = CrackedConstants.PROVIDER_ID;
		CrackedSettings settings = settingsProvider.get();
		boolean requireRepeat = settings != null
				&& settings.getScenario() != null
				&& settings.getScenario().getRegistration() != null
				&& settings.getScenario().getRegistration().isRequireRepeat();

		registerForBothFlows(
				builder,
				PipelineScope.REGISTRATION,
				providerId,
				PipelineType.REGISTRATION,
				validationStep
		);
		registerForBothFlows(
				builder,
				PipelineScope.REGISTRATION,
				providerId,
				PipelineType.REGISTRATION,
				crackedAccountPresenceStep
		);
		registerForBothFlows(
				builder,
				PipelineScope.REGISTRATION,
				providerId,
				PipelineType.REGISTRATION,
				crackedRegistrationStep
		);
		if (requireRepeat) {
			registerForBothFlows(
					builder,
					PipelineScope.REGISTRATION,
					providerId,
					PipelineType.REGISTRATION,
					crackedRegistrationConfirmStep
			);
		}

		registerForBothFlows(
				builder,
				PipelineScope.AUTHENTICATION,
				providerId,
				PipelineType.AUTHENTICATION,
				validationStep
		);
		registerForBothFlows(
				builder,
				PipelineScope.AUTHENTICATION,
				providerId,
				PipelineType.AUTHENTICATION,
				sessionReuseStep
		);

		registerForBothFlows(
				builder,
				PipelineScope.AUTHENTICATION,
				providerId,
				PipelineType.AUTHENTICATION,
				authenticationPasswordStep
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

	private void registerForBothFlows(
			@NotNull PipelineExtensionBuilder builder,
			@NotNull PipelineScope scope,
			@NotNull String providerId,
			@NotNull PipelineType pipelineType,
			@NotNull Step step
	) {
		builder.registerStep(scope, providerId, StageType.PROVIDER, pipelineType, JourneyType.SEAMLESS, step);
		builder.registerStep(scope, providerId, StageType.PROVIDER, pipelineType, JourneyType.INTERACTIVE, step);
	}
}
