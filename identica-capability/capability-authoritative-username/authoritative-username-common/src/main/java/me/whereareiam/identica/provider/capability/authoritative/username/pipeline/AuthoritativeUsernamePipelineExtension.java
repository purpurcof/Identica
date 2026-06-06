package me.whereareiam.identica.provider.capability.authoritative.username.pipeline;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.pipeline.phase.PhasePlacement;
import me.whereareiam.identica.pipeline.extension.PipelineExtension;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionBuilder;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.prepare.*;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.authentication.SynchronizeAuthenticationUsernamePhase;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.migration.SynchronizeMigrationUsernamePhase;
import me.whereareiam.identica.provider.capability.authoritative.username.pipeline.scenario.registration.SynchronizeRegistrationUsernamePhase;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AuthoritativeUsernamePipelineExtension implements PipelineExtension {
	private final ApplyAuthoritativeUsernamePhase applyAuthoritativeUsernamePhase;
	private final SynchronizeAuthenticationUsernamePhase synchronizeAuthenticationUsernamePhase;
	private final SynchronizeRegistrationUsernamePhase synchronizeRegistrationUsernamePhase;
	private final SynchronizeMigrationUsernamePhase synchronizeMigrationUsernamePhase;
	private final ReviewAuthenticationUsernamePhase reviewAuthenticationUsernamePhase;
	private final ReviewRegistrationUsernamePhase reviewRegistrationUsernamePhase;
	private final PersistAuthenticationUsernamePhase persistAuthenticationUsernamePhase;
	private final PersistRegistrationUsernamePhase persistRegistrationUsernamePhase;

	@Override
	public @NotNull String id() {
		return "authoritative-username:phases";
	}

	@Override
	public void apply(@NotNull PipelineExtensionBuilder builder) {
		builder.registerPreparePhase("policy", applyAuthoritativeUsernamePhase, PhasePlacement.first());
		builder.registerScenarioPhase(
				PipelineType.AUTHENTICATION,
				"identity",
				synchronizeAuthenticationUsernamePhase,
				PhasePlacement.after("refresh-provider-profile")
		);
		builder.registerScenarioPhase(
				PipelineType.REGISTRATION,
				"identity",
				synchronizeRegistrationUsernamePhase,
				PhasePlacement.after("link-provider")
		);
		builder.registerScenarioPhase(
				PipelineType.MIGRATION,
				"identity",
				synchronizeMigrationUsernamePhase,
				PhasePlacement.after("apply-provider-link")
		);
		builder.registerScenarioPhase(
				PipelineType.AUTHENTICATION,
				"policy",
				reviewAuthenticationUsernamePhase,
				PhasePlacement.first()
		);
		builder.registerScenarioPhase(
				PipelineType.AUTHENTICATION,
				"policy",
				persistAuthenticationUsernamePhase,
				PhasePlacement.last()
		);
		builder.registerScenarioPhase(
				PipelineType.REGISTRATION,
				"policy",
				reviewRegistrationUsernamePhase,
				PhasePlacement.first()
		);
		builder.registerScenarioPhase(
				PipelineType.REGISTRATION,
				"policy",
				persistRegistrationUsernamePhase,
				PhasePlacement.last()
		);
	}
}
